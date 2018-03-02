
GF_SECURITY_ADMIN_PASSWORD=$(grep GF_SECURITY_ADMIN_PASSWORD ../docker-compose.yml | cut -d = -f 2 | cut -d '"' -f 1)

curl http://admin:${GF_SECURITY_ADMIN_PASSWORD}@localhost:3000/api/datasources -X POST -H 'Content-Type: application/json;charset=UTF-8' --data-binary '{"Name":"Prometheus","Type":"prometheus","url":"http://prometheus:9090","Access":"proxy","isDefault":true}'

for i in $(ls . | grep json); do
   curl http://admin:${GF_SECURITY_ADMIN_PASSWORD}@localhost:3000/api/dashboards/db -X POST -H 'Content-Type: application/json;charset=UTF-8' --data-binary "@$i"
done

curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://admin:$GRAFANA_PASSWORD@localhost:3000/api/datasources \
-d @- << EOF
{
  "Name": "Prometheus",
  "Type": "prometheus",
  "url": "http://prometheus:9090",
  "Access": "proxy",
  "isDefault": true
}
EOF


for i in $(ls grafana | grep json); do
   curl http://admin:foobar@serving-demo.hydrosphere.io:3000/api/dashboards/db \
      -X POST -H 'Content-Type: application/json;charset=UTF-8' --data-binary "@./grafana/docker_host.json"
done