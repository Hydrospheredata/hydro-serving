#!/bin/sh

[ -z "$APP_HTTP_PORT" ] && APP_HTTP_PORT="9090"
[ -z "$SIDECAR_HTTP_PORT" ] && SIDECAR_HTTP_PORT="8080"
[ -z "$SIDECAR_ADMIN_PORT" ] && SIDECAR_ADMIN_PORT="8082"

[ -z "$ZIPKIN_ENABLED" ] && ZIPKIN_ENABLED="false"
[ -z "$ZIPKIN_HOST" ] && ZIPKIN_HOST="zipkin"
[ -z "$ZIPKIN_PORT" ] && ZIPKIN_PORT="9411"
[ -z "$TRACING_OP"] && TRACING_OP="ingress"

[ -z "$MANAGER_HOST" ] && MANAGER_HOST="localhost"
[ -z "$MANAGER_PORT" ] && MANAGER_PORT=$APP_HTTP_PORT

cat <<EOF >> /hydro-serving/sidecar/envoy.json
{
  "listeners": [
    {
      "address": "tcp://0.0.0.0:$SIDECAR_HTTP_PORT",
      "filters": [
        {
          "type": "read",
          "name": "http_connection_manager",
          "config": {
            "tracing": {
              "operation_name": "$TRACING_OP"
            },
            "codec_type": "http1",
            "idle_timeout_s": 840,
            "stat_prefix": "ingress_http",
            "use_remote_address": true,
            "server_name":"hydro-serving",
            "rds":{
              "cluster": "global-cluster-manager",
              "route_config_name": "http",
              "refresh_delay_ms": 5000
            },
            "filters": [
              {
                "type": "decoder",
                "name": "router",
                "config": {}
              }
            ]
          }
        }
      ]
    }
  ],
EOF

if [ "$ZIPKIN_ENABLED" == "true" ]; then
cat <<EOF >> /hydro-serving/sidecar/envoy.json
  "tracing": {
    "http": {
      "driver": {
        "type": "zipkin",
        "config": {
          "collector_cluster": "zipkin",
          "collector_endpoint": "/api/v1/spans"
        }
      }
    }
  },
EOF
fi

cat <<EOF >> /hydro-serving/sidecar/envoy.json
  "admin": {
    "access_log_path": "/var/log/hydro-serving/sidecar/admin_access.log",
    "address": "tcp://0.0.0.0:$SIDECAR_ADMIN_PORT"
  },
  "cluster_manager": {
    "clusters":[
      {
        "name": "global-cluster-manager",
        "connect_timeout_ms": 250,
        "type": "strict_dns",
        "lb_type": "round_robin",
        "hosts": [{"url": "tcp://$MANAGER_HOST:$MANAGER_PORT"}]
      }
EOF

if [ "$ZIPKIN_ENABLED" == "true" ]; then
cat <<EOF >> /hydro-serving/sidecar/envoy.json
      ,{
        "name": "zipkin",
        "connect_timeout_ms": 1000,
        "type": "strict_dns",
        "lb_type": "round_robin",
        "hosts": [
          {
            "url": "tcp://$ZIPKIN_HOST:$ZIPKIN_PORT"
          }
        ]
      }
EOF
fi


cat <<EOF >> /hydro-serving/sidecar/envoy.json
    ],
    "sds": {
      "cluster": {
        "name": "sds",
        "connect_timeout_ms": 250,
        "type": "strict_dns",
        "lb_type": "round_robin",
        "hosts": [{"url": "tcp://$MANAGER_HOST:$MANAGER_PORT"}]
      },
      "refresh_delay_ms": 5000
    },
    "cds": {
      "cluster": {
        "name": "cds",
        "connect_timeout_ms": 250,
        "type": "strict_dns",
        "lb_type": "round_robin",
        "hosts": [{"url": "tcp://$MANAGER_HOST:$MANAGER_PORT"}]
      },
      "refresh_delay_ms": 5000
    }
  }
}
EOF

echo "file generated"
