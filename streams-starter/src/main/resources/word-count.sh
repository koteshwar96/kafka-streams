#!/bin/bash

# Start Kafka Cluster(locally)
# Zookeeper: 
/usr/local/bin/zookeeper-server-start /usr/local/etc/zookeeper/zoo.cfg 

# Kafka: 
/usr/local/bin/kafka-server-start /usr/local/etc/kafka/server.properties

#list all topics
/usr/local/bin/kafka-topics --list --bootstrap-server localhost:9092

# create input topic
/usr/local/bin/kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 2 --topic word-count-input
or
kafka-topics --create --topic demo-first-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# create output topic
/usr/local/bin/kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 2 --topic word-count-output

# launch a Kafka consumer
/usr/local/bin/kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic word-count-output \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer

# launch the streams application

# then produce data to it
/usr/local/bin/kafka-console-producer --bootstrap-server localhost:9092 --topic word-count-input

# list all topics that we have in Kafka (so we can observe the internal topics)
bin/kafka-topics.sh --list --zookeeper localhost:2181
