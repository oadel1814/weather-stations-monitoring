#!/bin/bash

# 1. Cleanup existing resources
echo "Cleaning up existing resources..."
kubectl delete statefulset,deployment,service --all
kubectl delete pods --all --grace-period=0 --force

# 2. Wait a moment for the cluster to stabilize
echo "Waiting for cleanup to finalize..."
sleep 5

# 3. Apply Volumes (Crucial: Must come before Deployments/StatefulSets)
echo "Applying Persistent Volumes and Claims..."
kubectl apply -f k8s/storage.yaml 

# 4. Apply infrastructure components
echo "Deploying infrastructure..."
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/elasticsearch.yaml
kubectl apply -f k8s/kibana.yaml

# 5. Deploy consumers and producers
echo "Deploying application components..."
kubectl apply -f k8s/central-station.yaml
kubectl apply -f k8s/weather-stations.yaml

echo "Experiment started! Watching pods..."
kubectl get pods -w