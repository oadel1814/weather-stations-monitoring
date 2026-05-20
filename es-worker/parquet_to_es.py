import os
import time
import json
import requests
import pyarrow.parquet as pq

# Environment variables
PARQUET_DIR   = os.getenv("PARQUET_DIR",  "/data/parquet")
ES_URL        = os.getenv("ES_URL",       "http://elasticsearch:9200/_bulk")
INDEX_NAME    = os.getenv("INDEX_NAME",   "weather_statuses")
STATE_FILE    = os.getenv("STATE_FILE",   "/data/indexed_state.json")
POLL_INTERVAL = int(os.getenv("POLL_INTERVAL", "360"))

def load_state() -> dict:
    """Load indexed files state. Returns dict: filepath -> record_count."""
    if os.path.exists(STATE_FILE):
        try:
            with open(STATE_FILE, "r") as f:
                return json.load(f)
        except (json.JSONDecodeError, IOError):
            print("WARNING: State file corrupted, starting fresh.")
    return {}

def save_state(state: dict):
    """Atomically write state to disk using a temp file + rename."""
    tmp = STATE_FILE + ".tmp"
    try:
        with open(tmp, "w") as f:
            json.dump(state, f, indent=2)
        os.replace(tmp, STATE_FILE)
    except IOError as e:
        print(f"WARNING: Failed to save state: {e}")



def wait_for_elasticsearch():
    health_url = ES_URL.replace("/_bulk", "/_cluster/health")
    while True:
        try:
            r = requests.get(health_url, timeout=5)
            if r.status_code == 200:
                print("Elasticsearch is ready.")
                return
        except requests.exceptions.RequestException:
            pass
        print("Waiting for Elasticsearch...")
        time.sleep(5)

def send_to_es(rows: list, filename: str) -> bool:
    """Send rows to ES bulk API. Returns True on success."""
    body = ""
    for row in rows:
        station_id  = row.get("station_id")
        sequence_no = row.get("s_no")
        doc_id      = f"st_{station_id}_seq_{sequence_no}"
        body += json.dumps({"index": {"_index": INDEX_NAME, "_id": doc_id}}) + "\n"
        body += json.dumps(row) + "\n"

    try:
        response = requests.post(
            ES_URL,
            data=body,
            headers={"Content-Type": "application/x-ndjson"},
            timeout=30
        )
        if response.status_code == 200 and not response.json().get("errors"):
            return True
        else:
            print(f"✗ ES rejected {filename}: {response.text[:300]}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"✗ ES request failed for {filename}: {e}")
        return False


def poll_and_index():
    print(f"Checking {PARQUET_DIR} for new Parquet files...")

    # load state once per poll cycle — not on every file
    state = load_state()
    state_changed = False

    for root, dirs, files in os.walk(PARQUET_DIR):
        # sort files so we process them in chronological order
        for filename in sorted(files):
            if not filename.endswith(".parquet"):
                continue

            filepath = os.path.join(root, filename)

            # idempotency check, skip already indexed files
            if filepath in state:
                continue

            try:
                table = pq.read_table(filepath)
                rows  = table.to_pylist()

                if not rows:
                    print(f"Empty file, skipping: {filename}")
                    # mark empty files as indexed so we don't retry them forever
                    state[filepath] = {"records": 0, "indexed_at": time.time()}
                    state_changed = True
                    continue

                print(f"Reading {filename} ({len(rows)} records)...")

                if send_to_es(rows, filename):
                    state[filepath] = {
                        "records":    len(rows),
                        "indexed_at": time.time()
                    }
                    state_changed = True
                    print(f"✓ Indexed {filename} ({len(rows)} records)")

            except Exception as e:
                # file may still be written by Java — will retry next poll
                print(f"Skipping {filename} — may still be writing. Error: {e}")

    # save state once per poll cycle only if something changed
    if state_changed:
        save_state(state)
        print(f"State saved — {len(state)} files indexed total.")

if __name__ == "__main__":
    print("Starting Parquet-to-Elasticsearch worker...")
    wait_for_elasticsearch()
    while True:
        poll_and_index()
        time.sleep(POLL_INTERVAL)