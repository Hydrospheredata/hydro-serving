docker-compose \
    -f docker-compose/docker-compose.service.template \
    -f docker-compose/hydro-serving-manager.service.template \
    -f docker-compose/hydro-serving-gateway.service.template \
    -f docker-compose/hydro-serving-ui.service.template \
    -f docker-compose/sonar.service.template \
    -f docker-compose/hydro-auto-od.service.template \
    -f docker-compose/hydro-root-cause.service.template \
    -f docker-compose/hydro-stat.service.template \
    -f docker-compose/hydro-visualization.service.template \
    -f docker-compose/minio.service.template \
    -f docker-compose/mongo.service.template \
    -f docker-compose/postgresql.service.template \
    -f docker-compose/prometheus-am.service.template \
    config > docker-compose.yaml
