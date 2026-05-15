
# Weather Data Acquisition

This project implements:

1. Weather Stations Producers
2. Kafka Integration
3. Kafka Streams Rain Detection

## Features

- Random weather generation
- Battery distribution:
  - low 30%
  - medium 40%
  - high 30%
- 10% dropped messages
- Kafka Producer
- Kafka Streams processor
- Rain alerts topic

---

## Requirements

- Java 17
- Apache Kafka
- Maven

---

## Kafka Topics

Create topics:

```bash
kafka-topics.sh --create --topic weather-readings --bootstrap-server localhost:9092
kafka-topics.sh --create --topic rain-alerts --bootstrap-server localhost:9092
```

---

## Build

```bash
mvn clean package
```

---

## Run Producer

Linux/macOS:

```bash
export STATION_ID=1
mvn exec:java -Dexec.mainClass="com.data_aquisition.producer.WeatherStationProducer"
```

Windows CMD:

```cmd
set STATION_ID=1
mvn exec:java -Dexec.mainClass="com.data_aquisition.producer.WeatherStationProducer"
```

---

## Run Streams Processor

```bash
mvn exec:java -Dexec.mainClass="com.data_aquisition.streams.RainDetectorStream"
```

---

## Consume Weather Topic

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic weather-readings --from-beginning
```

---

## Consume Rain Alerts

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic rain-alerts --from-beginning
```
