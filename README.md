#Getting Started with Kafka
Use the steps below to setup up a local instance of Kafka for this example. THis is based off of apache-kafka_2.10-0.8.2.2.

##1. Locate and download Apache Kafka

Kafka can be located at this URL: [http://kafka.apache.org/downloads.html](http://kafka.apache.org/downloads.html)

You will want to download and install the binary version for Scala 2.10.

##2. Install Apache Kafka

Once downloaded you will need to extract the file. It will create a folder/directory. Move this to a location of your choice.

##3. Start ZooKeeper and Kafka

Start local copy of zookeeper

  * `<kafka home dir>bin/zookeeper-server-start.sh config/zookeeper.properties`

Start local copy of Kafka

  * `<kafka home dir>bin/kafka-server-start.sh config/server.properties`

##4. Prepare a message topic for use.

Create the topic we will use for the demo

  * `<kafka home dir>bin/kafka-topics.sh --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic stream_ts`

Validate the topic was created. 

  * `<kafka home dir>bin/kafka-topics.sh --zookeeper localhost:2181 --list`
  
##A Couple of other useful Kafka commands

Delete the topic. (Note: The server.properties file must contain `delete.topic.enable=true` for this to work)

  * `<kafka home dir>bin/kafka-topics.sh --zookeeper localhost:2181 --delete --topic stream_ts`
  
Show all of the messages in a topic from the beginning

  * `<kafka home dir>bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic stream_ts --from-beginning`
  
## Starting DSE tarball install on the local OSX or Linux machine

  * `dse cassandra -k -s -Dcassandra.logdir=~/Datastax/data/cassandra/log`
  
## Stating the ODBC driver in DSE tarball install on Local OSX or Linux

Since we are doing this on a laptop want to limit the resources the thrift server consumes.

  * `dse start-spark-sql-thriftserver --conf spark.cores.max=2`