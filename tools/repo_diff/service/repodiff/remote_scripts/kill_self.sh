#!/bin/bash

zoneMetadata=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/zone" -H "Metadata-Flavor:Google")
# Split on / and get the 4th element to get the actual zone name
IFS=$'/'
zoneMetadataSplit=($zoneMetadata)
zone="${zoneMetadataSplit[3]}"

echo $(hostname)
echo $zone

gcloud compute instances delete $(hostname) --zone=$zone --quiet
