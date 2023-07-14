# Booster Messaging

## Purpose

Booster messaging is a library that provides added functionality to use with Kafka and GCP pub/sub 
that helps addressing some of the problems one can encounter interacting with these systems.

There are two main issues when working with a messaging system, especially Kafa and GCP pub/sub:

1. Number of consumers are typically limited and does not allow one to utilize all available computing resources when size of cluster grows.
2. Back pressure typically isn't enabled with Java based libraries.

## Features

### Back Pressure

Booster messaging writes all pulled messaged to a ```BlockingQueue```, and from the queue, creates an 
on demand ```Flux``` when subscribers of the flux requests for more data. Actual message queue consumers 
are blocked when the ```BlockingQueue``` is full, and only can pull when the queue is empty, creating 
back pressure.

### Efficient Use of CPU Resource 

While the number of message queue consumers may be limited, the ```BlockingQueue``` consumers can 
be configured and each runs in a separate thread. If more CPU resources are available, one can 
simply create more ```BlockingQueue``` consumers.

### Integration with Booster Task 

A booster task is nothing but an asynchronously executed task, since booster messaging creates the 
pulled messages as a ```Flux```, one can simply use ```map``` or ```flatMap``` to handle each of the 
pulled messages with a ```Task```.

## Getting started

### Project Setup

## Usage 

### Terminology 

1. Publisher - publishers are responsible for sending a message to a messaging system. 
2. Subscriber - subscribers are used to receive messages either directly from messaging system, or via a provided messaging system consumer.
3. Processor - processors receive messages received by subscribers, and apply business logic to these messages.

### Message Consumption

**Note**:
1. Kafka consumers need to be set to manual acknowledge.
2. No deduplication is supported by subscribers. This typically requires a persistence storage and is expected to be provided in consumption logic.

Subscribers implement either ```SubscriberFlow``` where a stream of events are provided as a ```Flux```, or
a ```BatchSubscriberFlow``` where each event in a ```Flux``` is itself a list of events.

Internally, all subscriber provide a flux() method that allows one to consume the events coming 
from ```BlockingQueue``` in a reactive manner. The flux is created in an on demand fashion,
meaning only one a message is consumed, will one be pulled from the ```BlockingQueue```, which 
adds back pressure to the consumer chain.

#### GCP Pub/sub Subscriber 

In order to use GCP pub/sub subscriber, one needs to instantiate ```GcpPubSubPullSubscriber```. Pull 
mechanism is the only mechanism supported right now, as subscription based mechanism imposes
more restrictions on the type of service one can create.

GCP pub/sub subscribers by default pulls a list of events in a single request. Use either ```SubscriberFlow``` or 
```BatchedSubscriberFlow``` based on your needs.

#### Kafka Subscriber

Kafka subscribers only supports spring-kafka's listener mechanism. It also requires acknowledgement type 
to be manual. If Kafka processor is used, one does not need to manually acknowledge these messages
after processing.

### Publishers

All publishers behave similarly and returns a ```PublisherRecord``` when successful, or an exception 
if an error occurs during publishing.

#### GCP Pub/sub Publisher 

```GcpPublisher``` needs to be created, which expects a ```PubSubPublisherTemplate``` to be passed 
to complete actual message publishing.

#### Kafka Publisher 

Kafka message publishing supports both reactive way as well as spring default method. 

### Processor 

There are two types of processors:
1. Single message processor, where the input is a single message, or
2. Batch processor, where each input is a list of messages.

Kafka processor only support single message processor, while GCP pub/sub processor can either 
be a single message processor, or batch processor. The two types are defined as different classes, and
cannot be switched dynamically.

### Use in Your Program 

1. ```BoosterMessagingConfig``` is auto-configured bean that creates ```GcpPubSubSubscriberConfig``` and ```KafkaSubscriberConfig``` beans that load the configuration from config file.
2. Message processing components including publishers, subscribers, and processors are not defined as beans, but should be created in your service or component objects.
3. Use the auto-configured beans and corresponding constructors to create your publishers, subscribers and processors.
4. Subscribers provide ```SubscriberFlow``` or ```BatchSubscriberFlow``` which should be used by processors to receive messages.

### Metrics Reported

| Metric Name               | Type    | Tag            | Tag Values                | Description       |
|---------------------------|---------|----------------|---------------------------|-------------------|
| enqueue_count             | counter | name           | queue name                |                   |
|                           |         | status         | success or failure        |                   |
|                           |         | reason         | success or exception name | reason of failure |
| enqueue_time              | timer   | name           | queue name                |                   |
| dequeue_count             | counter | name           | queue name                |                   |
|                           |         | status         | success or failure        |                   |
|                           |         | reason         | success or exception name | reason of failure |
| dequeue_time              | timer   | name           | queue name                |                   |
| subscriber_pull_count     | counter | name           | subscriber name           |                   |
|                           |         | messaging_type | kafka or gcp_pubsub       |                   |
|                           |         | status         | success or failure        |                   |
|                           |         | reason         | success or exception name | reason of failure |
| subscriber_pull_time      | timer   | name           | subscriber name           |                   |
|                           |         | messaging_type | kafka or gcp_pubsub       |                   |
| send_count                | counter | name           | publisher name            |                   |
|                           |         | messaging_type | kafka or gcp_pubsub       |                   |
|                           |         | status         | success or failure        |                   |
|                           |         | reason         | success or exception name | reason of failure |
| send_time                 | timer   | name           | publisher name            |                   |
|                           |         | messaging_type | kafka or gcp_pubsub       |                   |
| subscriber_pull_count     | counter | name           | subscriber name           |                   |
|                           |         | messaging_type | kafka or gcp_pubsub       |                   |
|                           |         | status         | success or failure        |                   |
|                           |         | reason         | success or exception name | reason of failure |
| subscriber_pull_time      | timer   | name           | publisher name            |                   |
|                           |         | messaging_type | kafka or gcp_pubsub       |                   |
| acknowledge_count         | counter | name           | subscriber name           |                   |
|                           |         | messaging_type | kafka or gcp_pubsub       |                   |
|                           |         | status         | success or failure        |                   |
|                           |         | reason         | success or ack_failure    |                   |
| subscriber_process_count  | counter | name           | subscriber name           |                   |
|                           |         | messaging_type | kafka or gcp_pubsub       |                   |
|                           |         | status         | success or failure        |                   |
|                           |         | reason         | success or exception      |                   |
