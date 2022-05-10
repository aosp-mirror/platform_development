#!/bin/bash

image_id=$(docker build -q .)
container_id=$(docker run -d --entrypoint /usr/bin/sleep ${image_id} 60)
docker container exec ${container_id} zip /app.zip -r /app
docker container cp ${container_id}:/app.zip .
docker container stop ${container_id}
docker container rm ${container_id}