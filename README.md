# Weather Stations Monitoring

## Overview

A distributed, data-intensive IoT pipeline that simulates a network of 10 weather stations continuously streaming real-time weather readings to a central processing station. The system is built around a microservices architecture using modern data engineering technologies, applying 6 Enterprise Integration Patterns and deployed via Docker and Kubernetes.

---

## Architecture

![Pipeline Architecture](Pipeline%20Architecture.png)

The pipeline is composed of four stages:

**Data Acquisition** → **Message Broker** → **Central Station** → **Indexing**

- 10 weather stations produce JSON messages at 1 msg/sec each with a 10% random drop rate simulating real-world packet loss
- An Open-Meteo Channel Adapter acts as an 11th virtual station, pulling live weather data from the Open-Meteo REST API and feeding it into the same Kafka topic
- A Kafka Streams rain detector runs in parallel, filtering messages with humidity > 70% and routing them to a dedicated `rain-alerts` topic — completely decoupled from the main consumer
- The Central Station consumes from Kafka with manual offset commits and fans out concurrently to three independent sinks
- A Python ES worker polls Parquet files and indexes them idempotently into Elasticsearch for Kibana analytics

---

## Components

### `weather-station`
A Java application simulating a weather station producer. Each instance runs with a unique `STATION_ID` and emits a `WeatherMessage` every second with randomized humidity, temperature, wind speed, and battery status (30% low / 40% medium / 30% high). Messages are randomly dropped at a 10% rate to simulate network loss.

### `open-meteo-adapter`
A Java Channel Adapter that polls the [Open-Meteo API](https://open-meteo.com/) on a configurable interval and maps the response to the standard `WeatherMessage` schema before producing to Kafka. Coordinates are configurable via environment variables.

### `central-station`
A Java application that is the heart of the system. It consumes from the `weather-readings` Kafka topic and concurrently:
- Writes the latest reading per station to the BitCask store via `put(station_id, latest_reading)`
- Buffers incoming messages and flushes to date-partitioned Parquet files every 5 minutes or 10K records — whichever comes first
- Commits Kafka offsets manually after processing to guarantee at-least-once delivery

### `bitcask-core`
A custom Java implementation of the BitCask Riak key-value storage engine built from scratch, featuring:
- **Append-only segment files** for high-throughput writes
- **In-memory key directory** for O(1) reads
- **Hint files** for fast crash recovery without scanning data files
- **Scheduled compaction** that merges segment files while avoiding disruption to active readers
- **gRPC API** exposing `Get`, `GetAll`, and concurrent performance testing endpoints

### `bitcask-client`
A Python gRPC client for interacting with the BitCask server. Supports:
- `--view-all` — exports all keys and latest values to a timestamped CSV file
- `--view --key=STATION_ID` — prints the latest reading for a specific station
- `--perf --clients=N` — spawns N concurrent threads each querying all keys, outputting per-thread CSV files for performance benchmarking

### `es-worker`
A Python worker that polls the Parquet directory every 6 minutes, reads new files using `pyarrow`, and bulk-indexes records into Elasticsearch using an atomic JSON state file to guarantee idempotent indexing across restarts.

### `rain-detector`
A Kafka Streams application that reads from `weather-readings`, filters messages where humidity exceeds 70%, and forwards them to the `rain-alerts` topic in real-time — running independently of the Central Station.

---

## Enterprise Integration Patterns

| Pattern | Where Applied |
|---|---|
| **Polling Consumer** | Central Station Kafka consumer poll loop |
| **Channel Adapter** | Open-Meteo adapter bridging REST API → Kafka |
| **Dead Letter Channel** | Malformed/unparseable messages routed to `dead-letter` topic |
| **Invalid Message Channel** | Business-invalid messages (e.g. humidity > 100) routed to `invalid-messages` topic |
| **Idempotent Receiver** | ES worker uses `doc_id = st_{id}_seq_{s_no}` — safe to re-index |
| **Envelope Wrapper** | Messages wrapped with metadata envelope before producing to Kafka |

---

## Kibana Analytics

Two dashboards confirm the correctness of the simulation:

- **Battery distribution per station** — stacked bar chart confirming ~30% low / ~40% medium / ~30% high across all stations
- **Dropped messages per station** — computed as `max(s_no) - doc_count` per station, confirming ~10% drop rate

---

## How to Run

### Prerequisites
```bash
# increase vm.max_map_count for Elasticsearch
sudo sysctl -w vm.max_map_count=262144

# create required host directories
mkdir -p ./parquet-data ./bitcask-data ./elastic/data
```

### Docker Compose
```bash
docker compose up --build
```

Services started:
- Kafka (KRaft mode, no Zookeeper)
- Elasticsearch + Kibana
- Central Station
- 10 Weather Stations + Open-Meteo Adapter
- Rain Detector
- ES Worker

### Kubernetes
```bash
./k8sRunScript.sh
```

The script cleans existing resources, applies persistent volumes, deploys infrastructure (Kafka, Elasticsearch, Kibana, BitCask), then deploys the core application and edge stations.

Watch pods spin up:
```bash
kubectl get pods -w
```

Connect to the BitCask gRPC server locally:
```bash
kubectl port-forward svc/bitcask-server 50051:50051
```

Then run the BitCask client:
```bash
./bitcask_client.sh --view-all
./bitcask_client.sh --view --key=1
./bitcask_client.sh --perf --clients=100
```

---

## Environment Variables

| Variable | Service | Default | Description |
|---|---|---|---|
| `KAFKA_BROKER` | all | `kafka:29092` | Kafka bootstrap server |
| `STATION_ID` | weather-station | — | Unique station identifier |
| `BITCASK_DIR` | central-station | `/bitcask` | BitCask storage path |
| `OPEN_METEO_LAT` | open-meteo-adapter | `30.0444` | Latitude for API calls |
| `OPEN_METEO_LON` | open-meteo-adapter | `31.2357` | Longitude for API calls |
| `POLL_INTERVAL_SECONDS` | open-meteo-adapter | `60` | API polling interval |
| `PARQUET_DIR` | es-worker | `/data/parquet` | Parquet files root path |
| `POLL_INTERVAL` | es-worker | `360` | ES indexing poll interval (seconds) |
| `ES_URL` | es-worker | `http://elasticsearch:9200/_bulk` | Elasticsearch bulk endpoint |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21, Python 3.11 |
| Messaging | Apache Kafka (KRaft), Kafka Streams |
| Storage | Custom BitCask Riak, Apache Parquet (Avro) |
| Search | Elasticsearch 7.11, Kibana 7.11 |
| Communication | gRPC |
| Containerization | Docker, Kubernetes |
| Build | Maven 3.9 |