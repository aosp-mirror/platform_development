# build stage
FROM node:lts-alpine as build-stage
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY src ./src
COPY public ./public
COPY *.js .
COPY .env* .
COPY .eslint* .

RUN npm run build

# production stage
FROM ubuntu:20.04 as production-stage
RUN apt-get update && apt-get --no-install-recommends install -y python3.9 unzip xxd cgpt unzip openjdk-16-jre-headless zip less

WORKDIR /app
VOLUME [ "/app/target", "/app/output"]
COPY otatools.zip .
COPY --from=build-stage /app/dist ./dist
COPY *.py .

EXPOSE 8000
CMD ["python3.9", "web_server.py"]