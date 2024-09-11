# WatchWebApp

Local web UI to update golden files from a device.

This is deployed to https://motion.teams.x20web.corp.google.com/, and launched
via platform_testing/libraries/motion/golden/watch_web_app

## Development

Install dependencies with:
```
npm install
```

test via

```
ng serve
```

then launch the golden updater via
```
./watch_local_tests.py --client_url=http://localhost:4200/
```
