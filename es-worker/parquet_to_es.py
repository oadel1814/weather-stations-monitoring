import os
import time
import json
import requests
import pyarrow.parquet as pq

# Environment variables
PARQUET_DIR = os.getenv("PARQUET_DIR", "/data")
ES_URL = os.getenv("ES_URL", "http://elasticsearch:9200/_bulk")
INDEX_NAME = os.getenv("INDEX_NAME", "weather_statuses")
STATE_FILE = os.path.join(PARQUET_DIR, "indexed_state.txt")
POLL_INTERVAL = int(os.getenv("POLL_INTERVAL", "60"))

def load_state():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, "r") as f:
            return set(line.strip() for line in f)
    return set()

def save_state(filepath):
    with open(STATE_FILE, "a") as f:
        f.write(filepath + "\n")

def wait_for_elasticsearch():
    health_url = ES_URL.replace("/_bulk", "/_cluster/health")
    while True:
        try:
            response = requests.get(health_url, timeout=5)
            if response.status_code == 200:
                print("Elasticsearch is ready.")
                break
        except requests.exceptions.RequestException:
            pass
        print("Waiting for Elasticsearch to become available...")
        time.sleep(5)

def poll_and_index():
    print(f"Checking {PARQUET_DIR} for new partitioned Parquet datasets...")
    indexed = load_state()

    # Check top-level items in the /data directory instead of walking deep
    for item in os.listdir(PARQUET_DIR):
        filepath = os.path.join(PARQUET_DIR, item)

        # We only care about directories (this skips our indexed_state.txt file)
        # We also skip hidden folders or the 'parquet' folder if it's just a base dir
        if not os.path.isdir(filepath) or item.startswith('.') or item == 'parquet':
            continue

        # If we already indexed this batch folder, skip it
        if filepath in indexed:
            continue

        try:
            # Point PyArrow at the top-level batch folder.
            # It will automatically find and read the year=2026/... partitions!
            print(f"Reading dataset directory: {item}")
            table = pq.read_table(filepath)
            rows = table.to_pylist()

            if not rows:
                print(f"Dataset {item} is empty.")
                continue

            body = ""
            for row in rows:
                station_id = row.get("station_id")
                sequence_no = row.get("s_no")

                if station_id is not None and sequence_no is not None:
                    doc_id = f"st_{station_id}_seq_{sequence_no}"
                else:
                    doc_id = f"st_{station_id}_ts_{row.get('status_timestamp')}"

                action_metadata = {
                    "index": {
                        "_index": INDEX_NAME,
                        "_id": doc_id
                    }
                }

                body += json.dumps(action_metadata) + "\n"
                body += json.dumps(row) + "\n"

            print(f"Sending {len(rows)} records to Elasticsearch...")
            response = requests.post(
                ES_URL,
                data=body,
                headers={"Content-Type": "application/x-ndjson"},
                timeout=15 # slightly higher timeout for bulk inserts
            )

            if response.status_code == 200 and not response.json().get("errors"):
                save_state(filepath)
                indexed.add(filepath)
                print(f"✅ Successfully indexed dataset {item}")
            else:
                print(f"❌ Elasticsearch rejected payload for {item}: {response.text}")

        except Exception as e:
            print(f"Dataset {item} might still be writing. Will retry. Error: {str(e)}")

if __name__ == "__main__":
    print("Starting Parquet-to-Elasticsearch worker...")
    wait_for_elasticsearch()

    while True:
        poll_and_index()
        time.sleep(POLL_INTERVAL)