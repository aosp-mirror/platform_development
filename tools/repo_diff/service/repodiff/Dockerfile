FROM golang:1.10-alpine
ADD . /go/src/repodiff
RUN apk --no-cache add \
      curl \
      git \
      build-base \
      && curl https://raw.githubusercontent.com/golang/dep/5bdae264c61be23446d622ea84a1c97b895f78cc/install.sh | sh \
      && curl https://storage.googleapis.com/git-repo-downloads/repo > /bin/repo \
      && chmod a+x /bin/repo \
      && cd /go/src/repodiff \
      && dep ensure \
      && go install repodiff \
      && cp config.json /go/bin/ \
      && cp .gitcookies /go/bin/ \
      && cp .gitconfig /go/bin/ \
      && cp credentials.json /go/bin/

FROM alpine:3.7
RUN apk --no-cache add \
      bash \
      git \
      python \
      && mkdir -p /app_output
COPY --from=0 /bin/repo /bin/repo
WORKDIR /app
COPY --from=0 /go/bin/repodiff .
COPY --from=0 /go/bin/config.json .
COPY --from=0 /go/bin/.git* /root/
COPY --from=0 /go/bin/credentials.json .
COPY --from=0 /go/bin/credentials.json .
COPY --from=0 /go/src/repodiff/*py ./pytools/
COPY --from=0 /go/src/repodiff/*txt ./pytools/

ENV GCP_DB_INSTANCE_CONNECTION_NAME_DEV=$GCP_DB_INSTANCE_CONNECTION_NAME_DEV
ENV GCP_DB_USER_DEV=$GCP_DB_USER_DEV
ENV GCP_DB_PASSWORD_DEV=$GCP_DB_PASSWORD_DEV
ENV GCP_DB_NAME_DEV=$GCP_DB_NAME_DEV
ENV GCP_DB_PROXY_PORT_DEV=$GCP_DB_PROXY_PORT_DEV
ENV GCP_DB_INSTANCE_CONNECTION_NAME_PROD=$GCP_DB_INSTANCE_CONNECTION_NAME_PROD
ENV GCP_DB_USER_PROD=$GCP_DB_USER_PROD
ENV GCP_DB_PASSWORD_PROD=$GCP_DB_PASSWORD_PROD
ENV GCP_DB_NAME_PROD=$GCP_DB_NAME_PROD
ENV GCP_DB_PROXY_PORT_PROD=$GCP_DB_PROXY_PORT_PROD
ENV ROLE="prod"
ENV GOOGLE_APPLICATION_CREDENTIALS="/app/credentials.json"

CMD ["./repodiff"]
