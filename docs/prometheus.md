1. 
```
docker build --no-cache -t hydro-serving/prometheus integrations/automation/prometheus
```

2. 
```
export HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
docker run -p 9093:9090 -e MANAGER_HOST=$HOST_IP -e MANAGER_PORT=8080 hydro-serving/prometheus:latest
```