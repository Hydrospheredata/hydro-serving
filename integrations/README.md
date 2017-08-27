### Prometheus

```
docker run -e MANAGER_HOST=192.168.90.117 -e MANAGER_PORT=9090 -p 9091:9090 hydro-serving/prometheus:0.0.1

docker run -e GF_SECURITY_ADMIN_PASSWORD=foobar -e GF_USERS_ALLOW_SIGN_UP=false -p 3000:3000 grafana/grafana
```