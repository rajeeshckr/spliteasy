#!/usr/bin/env bash

sudo podman build -t localhost/spliteasy-backend:latest ~/spliteasy/backend

sudo podman build \
    --build-arg VITE_API_URL=http://192.168.1.30:9090 \
    -t localhost/spliteasy-webclient:latest \
    ~/spliteasy/web-client

sudo systemctl restart podman-spliteasy-backend.service
sudo systemctl restart podman-spliteasy-webclient.service
