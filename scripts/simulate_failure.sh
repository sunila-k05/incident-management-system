#!/bin/bash

BASE_URL="http://localhost:8080/api/signals"

echo "========================================"
echo " IMS Failure Simulation Script"
echo " Simulating: RDBMS outage → API failure"
echo " → MCP cascade"
echo "========================================"

echo ""
echo "[t=0s] RDBMS PRIMARY latency spike detected..."
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "RDBMS_PRIMARY_01",
    "componentType": "RDBMS",
    "signalType": "LATENCY_SPIKE",
    "value": 4500,
    "threshold": 500,
    "unit": "ms",
    "region": "us-east-1",
    "metadata": {"host": "db-01.internal", "query": "SELECT *"}
  }' | jq .

sleep 2

echo ""
echo "[t=2s] RDBMS connection pool exhausted..."
for i in {1..5}; do
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "RDBMS_PRIMARY_01",
    "componentType": "RDBMS",
    "signalType": "CONNECTION_POOL_EXHAUSTED",
    "value": 100,
    "threshold": 80,
    "unit": "%",
    "region": "us-east-1",
    "metadata": {"host": "db-01.internal", "pool_size": "100"}
  }' | jq .
done

sleep 3

echo ""
echo "[t=5s] API Gateway failing (DB is down)..."
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "API_GATEWAY_01",
    "componentType": "API_GATEWAY",
    "signalType": "ERROR_RATE_HIGH",
    "value": 85,
    "threshold": 5,
    "unit": "%",
    "region": "us-east-1",
    "metadata": {"host": "api-gw-01.internal", "error": "upstream_timeout"}
  }' | jq .

sleep 3

echo ""
echo "[t=8s] MCP Host going down (APIs failing)..."
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "MCP_HOST_01",
    "componentType": "MCP_HOST",
    "signalType": "HEALTH_CHECK_FAIL",
    "value": 0,
    "threshold": 1,
    "unit": "count",
    "region": "us-east-1",
    "metadata": {"host": "mcp-01.internal", "reason": "api_dependency_failed"}
  }' | jq .

sleep 2

echo ""
echo "[t=10s] Cache cluster memory pressure..."
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "CACHE_CLUSTER_01",
    "componentType": "CACHE",
    "signalType": "MEMORY_PRESSURE",
    "value": 92,
    "threshold": 80,
    "unit": "%",
    "region": "us-east-1",
    "metadata": {"host": "redis-01.internal"}
  }' | jq .

sleep 2

echo ""
echo "[t=12s] Async Queue depth growing..."
curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "ASYNC_QUEUE_01",
    "componentType": "ASYNC_QUEUE",
    "signalType": "QUEUE_DEPTH_HIGH",
    "value": 45000,
    "threshold": 10000,
    "unit": "count",
    "region": "us-east-1",
    "metadata": {"host": "queue-01.internal", "queue": "events.processing"}
  }' | jq .

echo ""
echo "========================================"
echo " Simulation complete!"
echo " Check dashboard: http://localhost:3000"
echo " Check health:    http://localhost:8080/health"
echo "========================================"
