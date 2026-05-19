FROM python:3.11-slim

WORKDIR /app

# Install the required data engineering libraries
RUN pip install --no-cache-dir pyarrow requests

COPY parquet_to_es.py .

# Run the script unbuffered so logs appear immediately in Docker/K8s
CMD ["python", "-u", "parquet_to_es.py"]