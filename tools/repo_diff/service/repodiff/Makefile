SQL_SCRIPT_DIR="tools/migrations"

DOCKER_SERVICE_NAME="repodiff-service"
DOCKER_CONTAINER_REGISTRY="gcr.io"
DOCKER_IMAGE_NAME="repodiff-image"
DOCKER_TAG_NAME="latest"
DOCKER_DOCKERFILE_DIR="."
DOCKER_TARGET_PORT=$(shell cat config.json | jq ".port")
DOCKER_CLUSTER_NAME="repodiff-default-cluster"
DOCKER_DEPLOYMENT_NAME="repodiff-deployment"
DOCKER_REPLICA_COUNT="1"

DOCKER_CANONICAL_ID=$(DOCKER_CONTAINER_REGISTRY)/$(GOOGLE_PROJECT_ID)/$(DOCKER_IMAGE_NAME):$(DOCKER_TAG_NAME)

PORT_HTTP="80"
GCE_ZONE="us-west1-b"
GCE_IMAGE_PROJECT="ubuntu-os-cloud"
GCE_IMAGE_FAMILY="ubuntu-1604-lts"

TMP_CREDENTIAL_FNAME=service_account_credentials.json

# https://cloud.google.com/compute/docs/machine-types
GCE_MACHINE_TYPE="n1-standard-64"

PROJECT_NAME="auto-diff-android-branches"
REMOTE_MACHINE_NAME=mithalop5
FIREWALL_NAME=public-http-access
DISK_SIZE=500GB
RUN_COMMAND_REMOTE=gcloud compute --project $(PROJECT_NAME) ssh --zone $(GCE_ZONE) "$(REMOTE_MACHINE_NAME)" --command
SCP_TO_HOST=gcloud compute --project $(PROJECT_NAME) scp --zone $(GCE_ZONE)
SERVICE_ACCOUNT_ID=repo-reader
SERVICE_ACCOUNT=$(SERVICE_ACCOUNT_ID)@$(PROJECT_NAME).iam.gserviceaccount.com

ifeq ($(ROLE),prod)
	GCP_DB_USER=$(GCP_DB_USER_PROD)
	GCP_DB_INSTANCE_CONNECTION_NAME=$(GCP_DB_INSTANCE_CONNECTION_NAME_PROD)
	GCP_DB_PASSWORD=$(GCP_DB_PASSWORD_PROD)
	GCP_DB_NAME=$(GCP_DB_NAME_PROD)
	GCP_DB_PROXY_PORT=$(GCP_DB_PROXY_PORT_PROD)
else
	GCP_DB_USER=$(GCP_DB_USER_DEV)
	GCP_DB_INSTANCE_CONNECTION_NAME=$(GCP_DB_INSTANCE_CONNECTION_NAME_DEV)
	GCP_DB_PASSWORD=$(GCP_DB_PASSWORD_DEV)
	GCP_DB_NAME=$(GCP_DB_NAME_DEV)
	GCP_DB_PROXY_PORT=$(GCP_DB_PROXY_PORT_DEV)
endif


bootstrap:
	mkdir -p $(GOPATH)/src
	./tools/setup_go_path_symlink.sh
	# include $GOPATH/bin as part of system path
	grep -q -F 'export PATH=$$PATH:$$GOPATH/bin' ~/.bashrc || echo 'export PATH=$$PATH:$$GOPATH/bin' >> ~/.bashrc
	source ~/.bashrc
	cd $(GOPATH)/src/repodiff; go get github.com/GoogleCloudPlatform/cloudsql-proxy/cmd/cloud_sql_proxy; \
		go get github.com/golang/dep/cmd/dep; \
		dep ensure; \
		go build

run:
	go build;
	ROLE="dev" ./repodiff

run_prod:
	go build;
	ROLE="prod" ./repodiff

reformat:
	go fmt .

db_shell:
	mysql -u $(GCP_DB_USER) -h 127.0.0.1 -P $(GCP_DB_PROXY_PORT) -p$(GCP_DB_PASSWORD) $(GCP_DB_NAME) ${EXTRA}

db_proxy:
	$(GOPATH)/bin/cloud_sql_proxy -instances=$(GCP_DB_INSTANCE_CONNECTION_NAME_DEV)=tcp:$(GCP_DB_PROXY_PORT_DEV) &
	$(GOPATH)/bin/cloud_sql_proxy -instances=$(GCP_DB_INSTANCE_CONNECTION_NAME_PROD)=tcp:$(GCP_DB_PROXY_PORT_PROD) &

db_proxy_ignore_err:
	make db_proxy; true

start_sql:
	make db_proxy_ignore_err &
	make db_shell < $(SQL_SCRIPT_DIR)/required_meta.sql

db_upgrade:
	make start_sql
	python tools/upgrade_db.py upgrade $(SQL_SCRIPT_DIR)


db_downgrade:
	make start_sql
	python tools/upgrade_db.py downgrade $(SQL_SCRIPT_DIR)

example:
	make db_shell EXTRA="-e 'DESCRIBE project_differential;'"

test:
	rm -rf build/
	ROLE="dev" go test ./... -v | grep -v PASS | grep -v RUN

sql_script:
	python tools/create_sql_script.py

ssh:
	 gcloud compute --project $(PROJECT_NAME) ssh --zone $(GCE_ZONE) $(REMOTE_MACHINE_NAME)

deploy:
	gcloud config set project $(PROJECT_NAME)
	@echo "Starting docker image build"
	make build_container_image
	@echo "Creating machine if it doesn't already exist"
	gcloud compute instances create $(REMOTE_MACHINE_NAME) \
		--machine-type $(GCE_MACHINE_TYPE) \
		--boot-disk-size $(DISK_SIZE) \
		--scopes https://www.googleapis.com/auth/source.read_only,https://www.googleapis.com/auth/compute \
		--zone $(GCE_ZONE) \
		--local-ssd interface=nvme \
		--metadata-from-file startup-script=remote_scripts/gce_startup.sh \
		--metadata AUTHOR=$(USER),SERVICE_ACCOUNT=$(SERVICE_ACCOUNT),GOOGLE_PROJECT_ID=$(GOOGLE_PROJECT_ID) \
		--image-project $(GCE_IMAGE_PROJECT) \
		--image-family $(GCE_IMAGE_FAMILY) \
		--min-cpu-platform skylake \
		--service-account $(SERVICE_ACCOUNT) \
		2>/dev/null || true
	@echo "Hackily waiting a bit for instance to start up"
	# TODO(slobdell) need to add a mechanism to block until startup script has completed
	@sleep 60
	./tools/clear_service_account_keys.py $(SERVICE_ACCOUNT) 2>/dev/null || true
	gcloud iam service-accounts keys create $(TMP_CREDENTIAL_FNAME) --iam-account $(SERVICE_ACCOUNT)
	$(RUN_COMMAND_REMOTE) 'mkdir -p /tmp/scripts'
	$(SCP_TO_HOST) remote_scripts/* "$(REMOTE_MACHINE_NAME)":/tmp/scripts/
	$(SCP_TO_HOST) $(TMP_CREDENTIAL_FNAME) "$(REMOTE_MACHINE_NAME)":/tmp/
	rm $(TMP_CREDENTIAL_FNAME)
	@echo "Stopping all existing docker images"
	$(RUN_COMMAND_REMOTE) 'docker stop $$(docker ps -a -q)' 2>/dev/null || true
	docker image save $(DOCKER_CANONICAL_ID) -o transferrable_docker_image.tar \
	  && $(SCP_TO_HOST) transferrable_docker_image.tar "$(REMOTE_MACHINE_NAME)":~/transferred_docker_image.tar \
    && $(RUN_COMMAND_REMOTE) 'docker load -i transferred_docker_image.tar' \
    && $(RUN_COMMAND_REMOTE) 'docker run -d --rm -p $(DOCKER_TARGET_PORT):$(DOCKER_TARGET_PORT) $(DOCKER_CANONICAL_ID)' \
		&& gcloud compute firewall-rules create $(FIREWALL_NAME) --allow tcp:$(DOCKER_TARGET_PORT) 2>/dev/null || true \
		&& gcloud compute firewall-rules update $(FIREWALL_NAME) --allow tcp:$(DOCKER_TARGET_PORT) --source-tags="$(REMOTE_MACHINE_NAME)" --source-ranges=0.0.0.0/0 --description="Allow requests over HTTP"
	@make output_instance_url --no-print-directory
	@rm transferrable_docker_image.tar

output_instance_url:
	@echo "Monitor progress at http://"$(shell (gcloud compute instances list | grep $(REMOTE_MACHINE_NAME) | awk -F ' ' '{print $$5}')):$(DOCKER_TARGET_PORT)/health

destroy:
	gcloud compute instances delete $(REMOTE_MACHINE_NAME) --zone $(GCE_ZONE) --quiet

############## DOCKER DEPLOYMENT
build_container_image:
	mkdir -p ./build
	# move contents of entire directory into build
	find . -type f -not -path 'build' -exec cp --parents '{}' 'build/' \;
	# copy python scripts repo uses, TODO re-structure codebase so the dependencies align with file structure
	cp ../../*.{txt,py} build/
	# Application credentials must be downloaded from https://pantheon.corp.google.com; set this environment variable to the path of the downloaded file
	cp $(GOOGLE_APPLICATION_CREDENTIALS) build/
	# copy local environment variables into Dockerfile
	cat Dockerfile | envsubst > build/Dockerfile
	# copy permissions from local gitcookies into Dockerfile (container will sync repo as you)
	cp static/docker_git_config ./build/.gitconfig
	cp ~/.gitcookies ./build/.gitcookies
	docker build -t $(DOCKER_CANONICAL_ID) ./build
	rm -rf ./build

docker_shell:
	docker run -it --rm $(DOCKER_CANONICAL_ID) bash

upload_container_image:
	gcloud config set project $(GOOGLE_PROJECT_ID)
	gcloud docker -- push $(DOCKER_CANONICAL_ID)

run_container_local:
	docker run --rm -p $(DOCKER_TARGET_PORT):$(DOCKER_TARGET_PORT) $(DOCKER_CANONICAL_ID)

create_container_cluster:
	gcloud container clusters create $(DOCKER_CLUSTER_NAME) \
		--num-nodes=3 \
		--machine-type $(GCE_MACHINE_TYPE) \
	  --zone $(GCE_ZONE)

create_container_cluster_verify:
	gcloud compute instances list

expose_to_internets:
	kubectl expose deployment $(DOCKER_DEPLOYMENT_NAME) --type=LoadBalancer --port $(PORT_HTTP) --target-port $(DOCKER_TARGET_PORT)

expose_to_internets_verify:
	kubectl get service

scale:
	kubectl scale deployment $(DOCKER_DEPLOYMENT_NAME) --replicas=$(DOCKER_REPLICA_COUNT)

cleanup:
	kubectl delete service
############## END DOCKER DEPLOYMENT
