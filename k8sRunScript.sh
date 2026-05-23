#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e


echo "  Starting Weather Stations Cluster Deployment"


# 1. Cleanup existing resources
echo " Cleaning up existing Kubernetes resources..."
# Note: Added 'pvc' to the end of this list if you want to wipe data every time.
# For now, it leaves your data intact between runs.
kubectl delete statefulset,deployment,service --all
kubectl delete pods --all --grace-period=0 --force

# 2. Wait a moment for the cluster to stabilize
echo " Waiting for cleanup to finalize..."
sleep 5

# 3. Apply Volumes (Crucial: Must come before Deployments/StatefulSets)
echo "Applying Persistent Volumes and Claims..."
kubectl apply -f k8s/storage.yaml

# 4. Apply infrastructure and storage engines
echo "Deploying infrastructure (Kafka, Elastic, Kibana, Bitcask)..."
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/elasticsearch.yaml
kubectl apply -f k8s/kibana.yaml
kubectl apply -f k8s/bitcask-server.yaml

# Give infrastructure a few seconds to register before hitting it with the app
sleep 5

# 5. Deploy Core Application
echo " Deploying Central Station..."
kubectl apply -f k8s/central-station.yaml

# 6. Deploy consumers and producers
echo "] Deploying Workers and Edge Stations..."
kubectl apply -f k8s/es-worker.yaml
kubectl apply -f k8s/weather-stations.yaml
kubectl apply -f k8s/weather-station-api.yaml

echo "================================================="
echo "================================================="
echo ""
echo "To watch your pods spin up, run:"
echo "  kubectl get pods -w"
echo ""
echo "To connect your local Python script, open a new terminal and run:"
echo "  kubectl port-forward svc/bitcask-server 50051:50051"
echo "================================================="