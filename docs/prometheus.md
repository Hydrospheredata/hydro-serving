#####  Build docker with `Prometheus`
```
docker build --no-cache -t hydro-serving/prometheus integrations/automation/prometheus
```

##### Start `Prometheus`
```
export HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
docker run -p 9093:9090 -e MANAGER_HOST=$HOST_IP -e MANAGER_PORT=8080 hydro-serving/prometheus:latest
```

##### Start `Grafana`
```
docker run -p 3000:3000 -e GF_SECURITY_ADMIN_PASSWORD=foobar -e GF_USERS_ALLOW_SIGN_UP=false grafana/grafana:4.6.3
```

##### Add `Prometheus` as Source to `Grafana`
```
export HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://admin:foobar@$HOST_IP:3000/api/datasources \
-d @- << EOF
{
  "Name": "Prometheus",
  "Type": "prometheus",
  "url": "http://$HOST_IP:9093",
  "Access": "proxy",
  "isDefault": true
}
EOF
```

##### Add new Dashboard to `Grafana`
```
export HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
curl --header 'Content-Type: application/json' \
 --header 'Accept: */*' \
 -X POST http://admin:foobar@$HOST_IP:3000/api/dashboards/db \
 --data-binary @./integrations/automation/grafana/local/models.json
```

##### Open new [Dashboard http://localhost:3000/dashboard/db/simple-models](http://localhost:3000/dashboard/db/simple-models)