package com.learning.streams.starter;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import java.util.List;
import java.util.Properties;

@Slf4j
public class StreamsStarterApp {
    public static void main(String[] args) {
        log.info("Hello world!");

        Properties streamsConfig = new Properties();
        streamsConfig.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        streamsConfig.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "word-count");
        streamsConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        streamsConfig.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfig.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        // creating stream from topic
        KStream<String,String> kStream = streamsBuilder.stream("word-count-input");

        // Doing transformations
        KTable<String, Long> wordCount = kStream.mapValues(value -> value.toLowerCase())
                .flatMapValues(value -> List.of(value.split(" ")))
                .selectKey((key,value)->value)
                .groupByKey()
                .count(Materialized.as("counts-store"));

        // writing to a topic from KTable
        wordCount.toStream().to( "word-count-output", Produced.with(Serdes.String(), Serdes.Long()));


        Topology topology = streamsBuilder.build();
        KafkaStreams streams = new KafkaStreams(topology, streamsConfig);
        streams.start();

        // Debug what all info is as part of streamsBuilder, printing streams topology
        log.info("stream topology is: {}",topology.describe());

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));


    }
}