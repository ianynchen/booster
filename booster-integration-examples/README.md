# Booster 2 Examples

## Purpose

The examples illustrates how to use different Booster components, as well as configuring 
booster applications to send span data to Grafana Tempo.

## Get Started 

Setup the environment.

```shell
docker-compose up -d
```

The above command will start Prometheus, Grafana, Loki, Tempo and a local GCP
pub/sub server.

Go to ```http://localhost:3000``` to verify Grafana is setup correctly.

Go to **Configuration**, **Data Sources** in Grafana console. If data sources are 
not configured or anyone of Loki, Tempo, Prometheus is missing, add the missing 
data sources with the following links

1. Prometheus: ```http://prometheus:9090```
2. Tempo: ```http://tempo:8000```
3. Loki: ```http://loki:3100```

Go to Loki, change Regex for TraceID to ```(?:.*?\[[^\,]*\,)([^\,]*)```
