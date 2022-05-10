# OTAGUI

## Introduction
OTAGUI is a web interface for ota_from_target_files. Currently, it can only run locally.

OTAGUI use VUE.js as a frontend and python as a backend interface to ota_from_target_files.

## Usage
First, download the AOSP codebase and set up the environment variable in the root directory:
```
source build/envsetup.sh
lunch 17
```
In this case we use `lunch 17` as an example (aosp-x86_64-cf), you can choose whatever suitable for you.

Then, in this directory, please use `npm build` to install the dependencies.

Create a `target` directory to store the target files and a `output` directory
to store the output files:
```
mkdir target
mkdir output
```

Finally, run the python http-server and vue.js server:
```
python3 web_server.py &
npm run serve
```
### Run with Docker

1. Build the image `docker build -t zhangxp1998/test .`

2. Run: `docker run -it -p 8000:8000 -v target:/app/target -v output:/app/output zhangxp1998/test:latest`
