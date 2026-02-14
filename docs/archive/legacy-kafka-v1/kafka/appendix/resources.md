# External Resources

Curated list of external resources for Apache Kafka and Spring Kafka.

## Official Documentation

### Apache Kafka

- **Official Website**: [kafka.apache.org](https://kafka.apache.org)
- **Documentation**: [kafka.apache.org/documentation](https://kafka.apache.org/documentation/)
- **Quickstart**: [kafka.apache.org/quickstart](https://kafka.apache.org/quickstart)
- **Configuration**: [kafka.apache.org/documentation/#configuration](https://kafka.apache.org/documentation/#configuration)
- **Operations**: [kafka.apache.org/documentation/#operations](https://kafka.apache.org/documentation/#operations)

### Spring Kafka

- **Official Documentation**: [docs.spring.io/spring-kafka](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- **API Javadoc**: [docs.spring.io/spring-kafka/docs/current/api](https://docs.spring.io/spring-kafka/docs/current/api/)
- **GitHub Repository**: [github.com/spring-projects/spring-kafka](https://github.com/spring-projects/spring-kafka)
- **Spring Boot Kafka**: [docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.kafka](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.kafka)

### Confluent (Kafka Distribution)

- **Confluent Platform**: [docs.confluent.io](https://docs.confluent.io/platform/current/overview.html)
- **Kafka Connect**: [docs.confluent.io/kafka-connect](https://docs.confluent.io/kafka-connect/current/overview.html)
- **Schema Registry**: [docs.confluent.io/schema-registry](https://docs.confluent.io/platform/current/schema-registry/index.html)
- **ksqlDB**: [docs.confluent.io/ksqldb](https://docs.ksqldb.io/)

---

## Learning Resources

### Books

**Kafka: The Definitive Guide** (2nd Edition)
- Authors: Gwen Shapira, Todd Palino, Rajini Sivaram, Krit Petty
- Publisher: O'Reilly Media
- Focus: Comprehensive Kafka guide from basics to advanced
- Link: [oreilly.com](https://www.oreilly.com/library/view/kafka-the-definitive/9781492043072/)

**Kafka Streams in Action** (2nd Edition)
- Authors: William Bejeck
- Publisher: Manning Publications
- Focus: Real-time stream processing with Kafka Streams
- Link: [manning.com](https://www.manning.com/books/kafka-streams-in-action-second-edition)

**Designing Event-Driven Systems**
- Author: Ben Stopford
- Publisher: Confluent/O'Reilly
- Focus: Event-driven architecture patterns
- Link: [Free download from Confluent](https://www.confluent.io/designing-event-driven-systems/)

### Online Courses

**Kafka for Beginners**
- Platform: Udemy
- Instructor: Stephane Maarek
- Duration: ~10 hours
- Link: [udemy.com/course/apache-kafka](https://www.udemy.com/course/apache-kafka/)

**Spring Framework and Spring Boot**
- Platform: Spring Academy
- Free certification courses
- Link: [spring.academy](https://spring.academy/)

**Kafka Tutorials**
- Platform: Confluent Developer
- Interactive tutorials and code examples
- Link: [developer.confluent.io/tutorials](https://developer.confluent.io/tutorials/)

---

## Tutorials and Guides

### Baeldung (Java/Spring)

- **Spring Kafka Introduction**: [baeldung.com/spring-kafka](https://www.baeldung.com/spring-kafka)
- **Kafka with Spring Boot**: [baeldung.com/spring-boot-kafka-testing](https://www.baeldung.com/spring-boot-kafka-testing)
- **Kafka Consumer Examples**: [baeldung.com/kafka-exactly-once](https://www.baeldung.com/kafka-exactly-once)

### Apache Kafka Examples

- **Official Examples**: [github.com/apache/kafka/tree/trunk/examples](https://github.com/apache/kafka/tree/trunk/examples)
- **Spring Kafka Samples**: [github.com/spring-projects/spring-kafka/tree/main/samples](https://github.com/spring-projects/spring-kafka/tree/main/samples)

### Confluent Tutorials

- **Kafka 101**: [developer.confluent.io/learn-kafka](https://developer.confluent.io/learn-kafka/)
- **Stream Processing**: [developer.confluent.io/learn-kafka/kafka-streams](https://developer.confluent.io/learn-kafka/kafka-streams/)
- **Event-Driven Microservices**: [developer.confluent.io/microservices](https://developer.confluent.io/microservices/)

---

## Tools and Utilities

### Kafka Management

**Kafka UI Tools**:
- **Kafka UI**: [github.com/provectus/kafka-ui](https://github.com/provectus/kafka-ui) - Web UI for Kafka management
- **Kafdrop**: [github.com/obsidiandynamics/kafdrop](https://github.com/obsidiandynamics/kafdrop) - Kafka cluster viewer
- **AKHQ**: [github.com/tchiotludo/akhq](https://github.com/tchiotludo/akhq) - Kafka GUI for topic/consumer management

**Command Line Tools**:
- **kcat (kafkacat)**: [github.com/edenhill/kcat](https://github.com/edenhill/kcat) - CLI producer/consumer
- **kafka-tools**: Built-in Kafka CLI tools (kafka-topics, kafka-console-consumer, etc.)

### Monitoring and Observability

**Prometheus + Grafana**:
- **JMX Exporter**: [github.com/prometheus/jmx_exporter](https://github.com/prometheus/jmx_exporter)
- **Kafka Exporter**: [github.com/danielqsj/kafka_exporter](https://github.com/danielqsj/kafka_exporter)
- **Grafana Dashboards**: [grafana.com/grafana/dashboards/kafka](https://grafana.com/grafana/dashboards/?search=kafka)

**Other Tools**:
- **Burrow**: [github.com/linkedin/Burrow](https://github.com/linkedin/Burrow) - Consumer lag monitoring
- **Kafka Manager (CMAK)**: [github.com/yahoo/CMAK](https://github.com/yahoo/CMAK) - Cluster management
- **Cruise Control**: [github.com/linkedin/cruise-control](https://github.com/linkedin/cruise-control) - Auto-balancing

### Testing

**Embedded Kafka**:
- **Spring Kafka Test**: Built-in `@EmbeddedKafka` annotation
- **Testcontainers Kafka**: [testcontainers.org/modules/kafka](https://www.testcontainers.org/modules/kafka/)

**Performance Testing**:
- **kafka-producer-perf-test**: Built-in Kafka performance tool
- **kafka-consumer-perf-test**: Built-in consumer benchmark
- **JMeter Kafka Plugin**: [github.com/GSLabDev/pepper-box](https://github.com/GSLabDev/pepper-box)

---

## Community and Support

### Forums and Discussion

- **Apache Kafka Mailing Lists**: [kafka.apache.org/contact](https://kafka.apache.org/contact)
- **Stack Overflow**: [stackoverflow.com/questions/tagged/apache-kafka](https://stackoverflow.com/questions/tagged/apache-kafka)
- **Confluent Community**: [forum.confluent.io](https://forum.confluent.io/)
- **Spring Community**: [spring.io/community](https://spring.io/community)

### Slack Channels

- **Apache Kafka**: [kafka.apache.org/contact#slack](https://kafka.apache.org/contact#slack)
- **Spring Community**: [spring.io/community/slack](https://spring.io/community/slack)

### Social Media

- **Kafka Twitter**: [@apachekafka](https://twitter.com/apachekafka)
- **Confluent Twitter**: [@confluentinc](https://twitter.com/confluentinc)
- **Spring Twitter**: [@springcentral](https://twitter.com/springcentral)

---

## Blogs and Articles

### Official Blogs

- **Apache Kafka Blog**: [kafka.apache.org/blog](https://kafka.apache.org/blog)
- **Confluent Blog**: [confluent.io/blog](https://www.confluent.io/blog/)
- **Spring Blog**: [spring.io/blog](https://spring.io/blog)

### Technical Blogs

- **Martin Kleppmann** (Distributed Systems): [martin.kleppmann.com](https://martin.kleppmann.com/)
- **Jay Kreps** (LinkedIn/Confluent): [linkedin.com/in/jaykreps](https://www.linkedin.com/in/jaykreps/)
- **Ben Stopford** (Event Streaming): [benstopford.com](https://www.benstopford.com/)

### Company Engineering Blogs

- **LinkedIn Engineering**: [engineering.linkedin.com/blog/topic/kafka](https://engineering.linkedin.com/blog/topic/kafka)
- **Uber Engineering**: [eng.uber.com](https://eng.uber.com/) (search for Kafka articles)
- **Netflix Tech Blog**: [netflixtechblog.com](https://netflixtechblog.com/)
- **Airbnb Engineering**: [medium.com/airbnb-engineering](https://medium.com/airbnb-engineering)

---

## GitHub Repositories

### Example Projects

**Spring Kafka Examples**:
- [Spring Kafka Samples](https://github.com/spring-projects/spring-kafka/tree/main/samples)
- [Event-Driven Microservices](https://github.com/benwilcock/event-driven-microservices-with-spring-boot-and-kafka)

**Production Patterns**:
- [Kafka Patterns](https://github.com/confluentinc/kafka-tutorials)
- [Microservices with Kafka](https://github.com/piomin/sample-spring-kafka-microservices)

### Libraries and Extensions

**Serialization**:
- [Avro](https://github.com/apache/avro) - Data serialization system
- [Protobuf](https://github.com/protocolbuffers/protobuf) - Google's serialization

**Kafka Extensions**:
- [Kafka Connect](https://github.com/apache/kafka/tree/trunk/connect) - Data integration
- [Kafka Streams](https://github.com/apache/kafka/tree/trunk/streams) - Stream processing
- [KSQL](https://github.com/confluentinc/ksql) - SQL for Kafka

---

## Videos and Presentations

### Conference Talks

**Kafka Summit**: [kafka-summit.org/sessions](https://www.kafka-summit.org/sessions)
- Annual Kafka conference recordings
- Best practices and case studies

**Spring One**: [springone.io](https://springone.io/)
- Spring framework conference
- Spring Kafka sessions

**QCon**: [qconferences.com](https://qconferences.com/)
- Software architecture conference
- Event-driven architecture talks

### YouTube Channels

- **Confluent**: [youtube.com/confluentinc](https://www.youtube.com/confluentinc)
- **Spring Developer**: [youtube.com/springdeveloper](https://www.youtube.com/springdeveloper)
- **Apache Kafka**: Various conference presentations

---

## Design Patterns and Best Practices

### Event-Driven Patterns

- **Event Sourcing**: [martinfowler.com/eaaDev/EventSourcing.html](https://martinfowler.com/eaaDev/EventSourcing.html)
- **CQRS**: [martinfowler.com/bliki/CQRS.html](https://martinfowler.com/bliki/CQRS.html)
- **Saga Pattern**: [microservices.io/patterns/data/saga.html](https://microservices.io/patterns/data/saga.html)

### Kafka Best Practices

- **Confluent Best Practices**: [docs.confluent.io/platform/current/kafka/deployment.html](https://docs.confluent.io/platform/current/kafka/deployment.html)
- **Kafka Performance**: [kafka.apache.org/documentation/#maximizingefficiency](https://kafka.apache.org/documentation/#maximizingefficiency)
- **Security**: [kafka.apache.org/documentation/#security](https://kafka.apache.org/documentation/#security)

---

## Related Technologies

### Message Brokers (Alternatives)

- **RabbitMQ**: [rabbitmq.com](https://www.rabbitmq.com/)
- **Apache Pulsar**: [pulsar.apache.org](https://pulsar.apache.org/)
- **NATS**: [nats.io](https://nats.io/)
- **Amazon Kinesis**: [aws.amazon.com/kinesis](https://aws.amazon.com/kinesis/)

### Stream Processing

- **Apache Flink**: [flink.apache.org](https://flink.apache.org/)
- **Apache Spark Streaming**: [spark.apache.org/streaming](https://spark.apache.org/streaming/)
- **Kafka Streams**: [kafka.apache.org/documentation/streams](https://kafka.apache.org/documentation/streams/)

### Data Integration

- **Debezium** (CDC): [debezium.io](https://debezium.io/)
- **Apache Camel**: [camel.apache.org](https://camel.apache.org/)
- **Spring Cloud Stream**: [spring.io/projects/spring-cloud-stream](https://spring.io/projects/spring-cloud-stream)

---

## Certifications

### Confluent Certifications

- **Confluent Certified Developer for Apache Kafka**: Entry-level certification
- **Confluent Certified Administrator for Apache Kafka**: Operations certification
- Link: [confluent.io/certification](https://www.confluent.io/certification/)

### Spring Certifications

- **Spring Professional Certification**: Covers Spring Boot, Spring Framework
- Link: [spring.io/certification](https://spring.io/certification)

---

## Reference Cards and Cheat Sheets

- **Kafka CLI Commands**: [github.com/lensesio/kafka-cheat-sheet](https://github.com/lensesio/kafka-cheat-sheet)
- **Spring Kafka Reference**: Internal [Quick Reference](/kafka/getting-started/quick-reference)
- **Kafka Configuration**: [kafka.apache.org/documentation/#configuration](https://kafka.apache.org/documentation/#configuration)

---

## Contributing to This List

Have a useful resource to add? Please contribute!

**How to contribute**:
1. Fork the SmartAdmin repository
2. Add your resource to this file
3. Submit a pull request

See [Contributing Guide](/kafka/appendix/contributing) for details.

---

## See Also

- [Quick Start](/kafka/getting-started/quick-start) - Get started with SmartAdmin Kafka
- [Glossary](/kafka/appendix/glossary) - Kafka terminology
- [Changelog](/kafka/appendix/changelog) - Version history

---

**Last Updated**: 2026-01-22
