#!/bin/bash

# mount an SSD for fast repo syncing
mkfs.ext4 -F /dev/nvme0n1
mkdir /ssd
mount /dev/nvme0n1 /ssd
chmod a+w /ssd

# configure Docker to run on the SSD
mkdir -p /ssd/docker
mkdir -p /etc/docker
echo "{\"graph\": \"/ssd/docker\"}" > /etc/docker/daemon.json

# install Docker
apt-get update
apt-get -qq -y --force-yes install docker.io
author=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/attributes/AUTHOR" -H "Metadata-Flavor: Google")
usermod -a -G docker $author

# authenticate to Google Cloud as service account
serviceAccount=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/attributes/SERVICE_ACCOUNT" -H "Metadata-Flavor: Google")
googleProjectID=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/attributes/GOOGLE_PROJECT_ID" -H "Metadata-Flavor: Google")
gcloud projects add-iam-policy-binding $googleProjectID --member serviceAccount:$serviceAccount --role roles/compute.instanceAdmin.v1
