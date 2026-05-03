#!/bin/bash

BASE_URL="http://localhost:8080/api/signals/batch"

echo "Sending 1000 signals in batches to test throughput..."

for i in {1..10}; do
  curl -s -X POST $BASE_URL \
    -H "Content-Type: application/json" \
    -d '[
      {"componentId":"RDBMS_PRIMARY_01","componentType":"RDBMS","signalType":"LATENCY_SPIKE","value":4500,"threshold":500,"unit":"ms","region":"us-east-1"},
      {"componentId":"CACHE_CLUSTER_01","componentType":"CACHE","signalType":"MEMORY_PRESSURE","value":92,"threshold":80,"unit":"%","region":"us-east-1"},
      {"componentId":"API_GATEWAY_01","componentType":"API_GATEWAY","signalType":"ERROR_RATE_HIGH","value":85,"threshold":5,"unit":"%","region":"us-east-1"},
      {"componentId":"MCP_HOST_01","componentType":"MCP_HOST","signalType":"HEALTH_CHECK_FAIL","value":0,"threshold":1,"unit":"count","region":"us-east-1"},
      {"componentId":"ASYNC_QUEUE_01","componentType":"ASYNC_QUEUE","signalType":"QUEUE_DEPTH_HIGH","value":45000,"threshold":10000,"unit":"count","region":"us-east-1"}
    ]' > /dev/null
  echo "Batch $i sent"
done

echo "Done! Check metrics in backend console."
