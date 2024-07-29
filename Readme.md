## Kafka Streams

### Start Kafka
```aidl
#Zookeeper
/usr/local/bin/zookeeper-server-start /usr/local/etc/zookeeper/zoo.cfg 

#Kafka
/usr/local/bin/kafka-server-start /usr/local/etc/kafka/server.properties 

#list all topics
/usr/local/bin/kafka-topics --list --bootstrap-server localhost:9092

# create input topic
/usr/local/bin/kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 2 --topic word-count-input
```

### Kafka Streams
Kafka Streams is a client library provided by Apache Kafka that enables the development of stream processing 
applications and microservices. It simplifies the process of building applications that continuously 
process streams of records and offers powerful capabilities for transforming, aggregating, and enriching data in real-time and produce to a topic

#### Kafka Streams Topology
A topology in Kafka Streams represents the directed graph of stream processing nodes (processors) that 
define the processing logic. The nodes are connected by edges that represent the data flow between processors.

**Components of a Topology:**
Source Processor: Reads data from a Kafka topic and passes it to downstream processors.
Stream Processor: Applies transformations, aggregations, or other processing logic to the data.
Sink Processor: Writes processed data back to a Kafka topic or external system.

```aidl
StreamsBuilder builder = new StreamsBuilder();

// Source processor: Reads data from the "input-topic"
KStream<String, String> textLines = builder.stream("input-topic", Consumed.with(Serdes.String(), Serdes.String()));

// Stream processor: Processes and counts the words
KTable<String, Long> wordCounts = textLines
    .flatMapValues(textLine -> List.of(textLine.toLowerCase().split("\\W+")))
    .groupBy((key, word) -> word)
    .count(Materialized.as("counts-store"));

// Sink processor: Writes the word counts to the "output-topic"
wordCounts.toStream().to("output-topic", Produced.with(Serdes.String(), Serdes.Long()));

// Build the topology
Topology topology = builder.build();

// Print the topology description
System.out.println(topology.describe());
```

### Internal Topics:
Kafka streams may create internal intermediate topics, These topics shouldn't be deleted/altered by us.These are prefixed by application.id param
Two types,
1. Repartition topics: in case keys are transformed
2. Changelog topics: in case we perform aggregations

**KStreams:**
All Inserts, Similar to log, infinite, unbounded data streams.
It can be considered as a changelog of a table, where each record in the stream captures  a state change in the table

when to use:
reading from a topic that's not compacted, if new data is partial or transactional

**KTables:**
Similar to the database tables. All upserts on non-null values, deletes on null values.
It can be considered as a snapshot at a point in time, of the latest value for each key in a stream

when to use: 
reading from a topic that's log compacted (Compaction in Apache Kafka is a cleanup policy applied 
to Kafka topics to ensure that only the latest value for each key 
within a partition is retained, removing older, obsolete records Ex: Aggregation), where every update is
self sufficient (like bank balance)

#### Operations
**Stateless:** It means that the result of a transformation depends only on the data
point we are processing. We Dont need to know what happened before
Ex: ' Multiply by 2'

**Stateful:** It means that the result of a transformation also depends on an external 
information - the state.
Ex: ' Word count operation'

**MapValues:**
only effects values; For KStreams and KTable

**Map:**
Affects both keys and values (Triggers re-partition), for KStreams only

**Filter / FilterNot:** Takes one record and produces one or zero record. 
Doesn't change key or value, doesn't trigger re-partition. For KStreams and KTables.

**FlatMapValues:**
Takes one record and produces zero or one or many records. 
doesnt change keys, for KStreams only

**FlatMap:**
Takes one record and produces zero or one or many records.
change keys, for KStreams only

**KStream Branch:**
allows you to split a single KStream into multiple KStream instances based on specified predicates.
The branch method takes an array of Predicate objects and returns an array of KStream objects. 
Each predicate is evaluated against each record in the original stream, 
and the record is routed to the first KStream for which the predicate returns true. 
If no predicates match, the record is discarded.

**Select Key:**
Assigns a new key to the record, triggers a re-partition.
Its good practice to isolate this transformation.

We can read a topic as KStream, KTable, GlobalKTable
```
KStream<String, String> kStream = builder.stream("topic-name", Consumed.with(Serdes.String(), Serdes.String()));
KTable<String, String> kTable = builder.table("topic-name", Consumed.with(Serdes.String(), Serdes.String()));
GlobalKTable<String, String> kGlobalTable = builder.globalTable("topic-name", Consumed.with(Serdes.String(), Serdes.String()));
```

We can write any KStream / KTable to kafka topic via .To or .through
```aidl
// write to a topic and terminal operation
kStream.to("output-topic", Produced.with(Serdes.String(), Serdes.String()));
kTable.toStream().to("output-topic", Produced.with(Serdes.String(), Serdes.String()));

// write to a topic and get a stream/table from topic, used when we want to do more transformations
KStream<String, String> resultStream = kStream.through("intermediate-topic", Produced.with(Serdes.String(), Serdes.String()));
KStream<String, String> resultStream = kTable.toStream().through("intermediate-topic", Produced.with(Serdes.String(), Serdes.String()));

```
Note:
Any anytime you change the key, the data has to be shuffled around.
It's called shuffling and incurs a network cost. But now they get data grouped by this new key.
So the whole idea behind it is that when you répartition a stream with a key change, the data is redistributed amongst all your streams application.
So, try to minimize the usage of these methods which might change keys and trigger repartition


Streams and KTables duality
```aidl
KStream to Ktable
KTable<String, Long> kTable = kStream.groupByKey().count();
// or write to a intermediate topic and read from topic as KTable

// KTable to stream
KStream<String, Long> kStream = kTable.toStream();

```