#!/bin/bash
set -e

BOOTSTRAP=${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}

TOPICS=(
  "transactions.created"
  "fraud.scored"
  "fraud.review.required"
  "fraud.confirmed"
  "fraud.falsepositive"
  "fraud.model.deployed"
  "fraud.retraining.requested"
)

for topic in "${TOPICS[@]}"; do
  kafka-topics --bootstrap-server "$BOOTSTRAP" \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions 3 \
    --replication-factor 1 2>/dev/null || true
  echo "Topic $topic ready"
done

echo "All topics created"
