package com.datastax.demo
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

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.streaming.{Milliseconds, StreamingContext, Time}
import org.apache.spark.streaming.kafka.KafkaUtils
import kafka.serializer.StringDecoder
import org.apache.spark.rdd.RDD
import java.sql.Timestamp

case class SensorEvent(edgeId: String, sensorId: String, epochHr: String, ts: Timestamp, depth: Double, value: Double)

// This implementation uses the Kafka Direct API supported in Spark 1.4
object SparkKafkaConsumer extends App {

  val appName = "SparkKafkaConsumer"

  val conf = new SparkConf()
    .set("spark.cores.max", "2")
    .set("spark.executor.memory", "512M")
    .setAppName(appName)
  val sc = SparkContext.getOrCreate(conf)

  val sqlContext = SQLContext.getOrCreate(sc)
  import sqlContext.implicits._
  import org.apache.spark.sql.functions._

  val ssc = new StreamingContext(sc, Milliseconds(1000))
  ssc.checkpoint(appName)

  val kafkaTopics = Set("stream_ts")
  val kafkaParams = Map[String, String]("metadata.broker.list" -> "localhost:9092")

  val kafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, kafkaTopics)

  kafkaStream
    .foreachRDD {
      (message: RDD[(String, String)], batchTime: Time) => {
        val df = message.map {
          case (k, v) => v.split(";")
        }.map(payload => {
          val ts = Timestamp.valueOf(payload(3))
          SensorEvent(payload(0), payload(1), payload(2), ts, payload(4).toDouble, payload(5).toDouble)
        }).toDF("edge_id", "sensor", "epoch_hr", "ts", "depth", "value")

        df
          .write
          .format("org.apache.spark.sql.cassandra")
          .mode(SaveMode.Append)
          .options(Map("keyspace" -> "demo", "table" -> "data"))
          .save()

        df.show(5)
        println(s"${df.count()} rows processed.")

        df
          .select("edge_id", "sensor", "ts", "depth", "value")
          .write
          .format("org.apache.spark.sql.cassandra")
          .mode(SaveMode.Append)
          .options(Map("keyspace" -> "demo", "table" -> "last"))
          .save()
      }
    }

  val windowMillis = 15000
  val sliderMillis = 5000
  val maRatio = windowMillis/(sliderMillis * 1.0)

  kafkaStream
    .window(Milliseconds(windowMillis), Milliseconds(sliderMillis))
    .foreachRDD {
      (message: RDD[(String, String)], batchTime: Time) => {
        val df = message.map {
          case (k, v) => v.split(";")
        }.map(payload => {
          val ts = Timestamp.valueOf(payload(3))
          SensorEvent(payload(0), payload(1), payload(2), ts, payload(4).toDouble, payload(5).toDouble)
        }).toDF("edge_id", "sensor", "epoch_hr", "ts", "depth", "value")

        val maxTs = df.select("ts").sort(desc("ts")).first().get(0).asInstanceOf[Timestamp]
        val offsetTs = new Timestamp(maxTs.getTime - sliderMillis)
        val recCount = df.filter(df("ts") > offsetTs).count()
        val maRecCount = df.count()/maRatio

        val dfCount = sc.makeRDD(Seq((1, maxTs, recCount, maRecCount))).toDF("pk", "ts", "count", "count_ma")
        dfCount.show()
        dfCount
          .write
          .format("org.apache.spark.sql.cassandra")
          .mode(SaveMode.Append)
          .options(Map("keyspace" -> "demo", "table" -> "count"))
          .save()
      }
    }

  ssc.start()
  ssc.awaitTermination()
}
