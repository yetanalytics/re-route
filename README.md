# re-route

Routing wrapper library for SPAs based on [re-frame](https://github.com/day8/re-frame) and [reitit](https://github.com/metosin/reitit).

## Run Demo

This library comes with a demo that demonstrates the functionality of re-route. This can be run via Figwheel or can be compiled and run on an nginx webserver.

To run via Figwheel, run `make dev`.

To compile the webpage, run `make prod`, which will create a `target/bundle` directory. Then to run the webpage on an nginx Docker image, run:
```
docker build -t re-route-nginx-image .
```
followed by
```
docker run --name re-route-nginx-container -p 9500:80 re-route-nginx-image
```
which will serve the webpage on `localhost:9500`.
