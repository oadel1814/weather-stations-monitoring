import argparse
import grpc
import time
import csv
import sys
import os
from concurrent.futures import ThreadPoolExecutor, as_completed

# Import generated gRPC code
from stubs import bitcask_pb2, bitcask_pb2_grpc

SERVER_ADDRESS = "localhost:50051"
OUTPUT_FOLDER = "csv_exports"


def fetch_all_and_write_csv(stub, timestamp, thread_id=None):
    try:
        os.makedirs(OUTPUT_FOLDER, exist_ok=True)

        if thread_id is not None:
            filename = f"{int(timestamp)}_thread_{thread_id}.csv"
        else:
            filename = f"{int(timestamp)}.csv"

        filepath = os.path.join(OUTPUT_FOLDER, filename)

        with open(filepath, mode="w", newline="", encoding="utf-8") as file:
            writer = csv.writer(file)
            writer.writerow(["key", "value"])

            # stub.GetAllKeys returns a stream of chunks
            chunk_stream = stub.GetAllKeys(bitcask_pb2.Empty())

            for chunk in chunk_stream:
                for pair in chunk.pairs:
                    writer.writerow([pair.key, pair.value])

        return True, filepath
    except grpc.RpcError as e:
        return False, f"gRPC Error: {e.details()}"
    except Exception as e:
        return False, f"Error: {str(e)}"


def get_single_key(stub, key):
    try:
        request = bitcask_pb2.KeyRequest(key=key)
        response = stub.GetKey(request)
        if response.found:
            print(response.value)
        else:
            print(f"Key '{key}' not found.", file=sys.stderr)
    except grpc.RpcError as e:
        print(f"gRPC Error: {e.details()}", file=sys.stderr)


def put_single_key(stub, key, value, silent=False):
    try:
        request = bitcask_pb2.PutRequest(key=key, value=value)
        response = stub.Put(request)

        # Only print the success/fail message if silent is False
        if not silent:
            if response.success:
                print(f"[SUCCESS] {response.message}")
            else:
                print(f"[FAILED] {response.message}", file=sys.stderr)

        return response.success
    except grpc.RpcError as e:
        if not silent:
            print(f"gRPC Error: {e.details()}", file=sys.stderr)
        return False


def trigger_merge(stub):
    try:
        print("Triggering database merge/compaction. This may take a moment...")
        response = stub.Merge(bitcask_pb2.Empty())
        if response.success:
            print(f"[SUCCESS] {response.message}")
        else:
            print(f"[FAILED] {response.message}", file=sys.stderr)
        return response.success
    except grpc.RpcError as e:
        print(f"gRPC Error: {e.details()}", file=sys.stderr)
        return False


def main():
    parser = argparse.ArgumentParser(description="Bitcask gRPC Client")
    parser.add_argument(
        "--view-all", action="store_true", help="Export all keys to CSV"
    )
    parser.add_argument("--view", action="store_true", help="View a specific key")
    parser.add_argument("--put", action="store_true", help="Insert or update a key")
    parser.add_argument("--merge", action="store_true", help="Trigger a database merge")
    parser.add_argument("--key", type=str, help="The target key")
    parser.add_argument(
        "--value", type=str, help="The value to insert (required for --put)"
    )
    parser.add_argument("--perf", action="store_true", help="Run performance test")
    parser.add_argument("--clients", type=int, help="Number of concurrent clients")

    parser.add_argument(
        "--test-keys",
        type=int,
        default=1000,
        help="Number of keys to use for the compaction test (default 1000)",
    )

    args = parser.parse_args()
    current_time = time.time()

    options = [("grpc.max_receive_message_length", 1024 * 1024 * 1024)]

    with grpc.insecure_channel(SERVER_ADDRESS, options=options) as channel:
        stub = bitcask_pb2_grpc.BitcaskServiceStub(channel)

        if args.view_all:
            success, msg = fetch_all_and_write_csv(stub, current_time)
            print(f"Generated: {msg}" if success else msg)

        elif args.view and args.key:
            get_single_key(stub, args.key)

        elif args.put and args.key and args.value:
            put_single_key(stub, args.key, args.value)

        elif args.merge:
            trigger_merge(stub)

        elif args.perf and args.clients:
            print(f"Running performance test with {args.clients} threads...")
            with ThreadPoolExecutor(max_workers=args.clients) as executor:
                futures = [
                    executor.submit(fetch_all_and_write_csv, stub, current_time, i)
                    for i in range(1, args.clients + 1)
                ]
                success_count = sum(
                    1 for future in as_completed(futures) if future.result()[0]
                )
            print(
                f"Performance test complete. Generated {success_count}/{args.clients} CSV files."
            )

        else:
            parser.print_help()


if __name__ == "__main__":
    main()
