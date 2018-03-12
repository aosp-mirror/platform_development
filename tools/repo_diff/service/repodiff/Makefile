SQL_SCRIPT_DIR="tools/migrations"

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
	./tools/setup_go_path_symlink.sh
	go get github.com/GoogleCloudPlatform/cloudsql-proxy/cmd/cloud_sql_proxy
	dep ensure
	go build;

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
	ROLE="dev" go test ./... -v

sql_script:
	python tools/create_sql_script.py
