# Fn Flow Examples - Saga Pattern

In which we will demonstrate how to build a massively-scalable, fault-tolerant, serverless app. There will be no mention of servers, networks, auto-scaling groups, operating systems, virtualisation or any other infrastructure. There will be no 'programming' in JSON. Just write code in your language.

## Saga

We will attempt to book a flight, hotel and car rental for our trip to San Francisco. We will clean up after ourselves if any of the stages fail. We will use fake APIs so that we can see what's going on.

## Local Demo Server Start-up

```sh
# Run a fn server:
docker run --rm -d -p 8080:8080 -v /var/run/docker.sock:/var/run/docker.sock -e NO_PROXY=172.17.0.2,172.17.0.3 -e LOG_LEVEL=debug --name functions fnproject/functions
    
# Run a fn flow completer:
docker run --rm -d -p 8081:8081 --name completer -e API_URL=http://172.17.0.2:8080/r -e NO_PROXY=172.17.0.2,172.17.0.3 fnproject/completer:latest

# Run a BS dashboard:
docker run --rm -d -p 3000:3000 --name bristol tteggel/bristol
```

## Build and Deploy the Demo App

```sh
fn apps create saga
fn apps config set saga COMPELTER_BASE_URL "http://172.17.0.3:8081"
fn deploy saga
```

## Run

Visit `http://localhost:3000/` in a browser.

```sh
# Run a successful booking:
cat sample-data.json | fn call saga /saga
```

You should see some calls appearing on the dashboard.

Use the dashboard to set a 500 response for one of the endpoints and re-run. You should see some compensating actions be performed.

## Clean Up
```sh
docker rm -f bristol functions completer
```
