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

Finally, run the python http-server and vue.js server:
```
python3 web_server.py &
npm run serve
```
