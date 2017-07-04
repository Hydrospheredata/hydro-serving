#!/bin/sh

cat <<EOF > /etc/hydro-serving/envoy.json
{
  "listeners": [
EOF

if [ "$USE_APP_HTTP" == "true" ]; then
cat <<EOF >> /etc/hydro-serving/envoy.json
    {
      "address": "tcp://0.0.0.0:$ENVOY_HTTP_PORT",
      "filters": [
        {
          "type": "read",
          "name": "http_connection_manager",
          "config": {
            "tracing": {
              "operation_name": "ingress"
            },
            "codec_type": "http1",
            "idle_timeout_s": 840,
            "stat_prefix": "egress_http1",
            "use_remote_address": true,
            "server_name":"$SERVICE_TYPE-$SERVICE_NAME-$SERVICE_VERSION",
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
    },
EOF
fi

cat <<EOF >> /etc/hydro-serving/envoy.json
    {
      "address": "tcp://0.0.0.0:$ENVOY_GRPC_PORT",
      "filters": [
        {
          "type": "read",
          "name": "http_connection_manager",
          "config": {
            "tracing": {
              "operation_name": "ingress"
            },
            "codec_type": "http2",
            "idle_timeout_s": 840,
            "stat_prefix": "egress_http2",
            "use_remote_address": true,
            "server_name":"$SERVICE_TYPE-$SERVICE_NAME-$SERVICE_VERSION",
            "rds":{
              "cluster": "global-cluster-manager",
              "route_config_name": "grpc",
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
cat <<EOF >> /etc/hydro-serving/envoy.json
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

cat <<EOF >> /etc/hydro-serving/envoy.json
  "admin": {
    "access_log_path": "/var/log/envoy/admin_access.log",
    "address": "tcp://0.0.0.0:$ENVOY_ADMIN_PORT"
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
cat <<EOF >> /etc/hydro-serving/envoy.json
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


cat <<EOF >> /etc/hydro-serving/envoy.json
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