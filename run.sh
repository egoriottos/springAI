#!/bin/bash

if [ -z "$OPENAI_API_KEY" ]; then
    echo "OPENAI_API_KEY не задан — пропиши переменную окружения перед запуском."
    exit 1
fi

./mvnw spring-boot:run