#!/bin/bash
cd /home/kavia/workspace/code-generation/ticket-data-metrics-service-91894-91903/ticket_metrics_api_backend
./gradlew checkstyleMain
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

