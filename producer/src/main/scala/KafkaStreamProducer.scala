/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by carybourgeois on 10/30/15.
 */
import java.sql.Timestamp
import java.util.Properties

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

class produceMessages(brokers: String, topic: String, edge_id: String, numSensors : Int) extends Actor {

  val sigma = 1
  val xbar = 0


  object kafka {
    val producer = {
      val props = new Properties()
      props.put("metadata.broker.list", brokers)
      props.put("serializer.class", "kafka.serializer.StringEncoder")

      val config = new ProducerConfig(props)
      new Producer[String, String](config)
    }
  }

  def receive = {
    case "send" => {
      val threshold = scala.util.Random.nextGaussian();

      val messages = for (sensor <- 1 to numSensors.toInt if scala.util.Random.nextGaussian() > threshold) yield {
        val sensor_id = sensor.toString
        val event_time = new Timestamp(System.currentTimeMillis())
        val epoch_hr = (event_time.getTime/3600000).toString
        val depth = (scala.util.Random.nextGaussian() * sigma + xbar)
        val metric = (scala.util.Random.nextGaussian() * sigma + xbar)
        val str = s"${edge_id};${sensor_id};${epoch_hr};${event_time.toString};${depth.toString};${metric.toString}"
        if (sensor < 5)
          println(str)

        new KeyedMessage[String, String](topic, str)
      }

      kafka.producer.send(messages: _*)
    }

    case _ => println("Not a valid message!")
  }
}

// Produces some random words between 1 and 100.
object KafkaStreamProducer extends App {

  /*
   * Get runtime properties from application.conf
   */
  val systemConfig = ConfigFactory.load()
  val kafkaHost = systemConfig.getString("KafkaStreamProducer.kafkaHost")
  println(s"kafkaHost $kafkaHost")
  val kafkaTopic = systemConfig.getString("KafkaStreamProducer.kafkaTopic")
  println(s"kafkaTopic $kafkaTopic")
  val numSensors = systemConfig.getInt("KafkaStreamProducer.numSensors")
  println(s"numSensors $numSensors")
  val edgeId = systemConfig.getString("KafkaStreamProducer.edgeId")
  println(s"edgeId $edgeId")
  val numRecords = systemConfig.getLong("KafkaStreamProducer.numRecords")
  println(s"numRecords $numRecords")
  val waitMillis = systemConfig.getLong("KafkaStreamProducer.waitMillis")
  println(s"waitMillis $waitMillis")

  /*
   * Set up the Akka Actor
   */
  val system = ActorSystem("KafkaStreamProducer")
  val messageActor = system.actorOf(Props(new produceMessages(kafkaHost, kafkaTopic, edgeId, numSensors)), name="genMessages")

  /*
   * Message Loop
   */
  var numRecsWritten = 0
  while(numRecsWritten < numRecords) {
    messageActor ! "send"

    numRecsWritten += numSensors

    println(s"${numRecsWritten} records written.")

    Thread sleep waitMillis
  }

}

