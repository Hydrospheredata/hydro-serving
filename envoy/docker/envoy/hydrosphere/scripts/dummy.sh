#!/bin/sh

SERVICE_ID=$1

echo "start $SERVICE_ID" >> /var/log/envoy/admin_access.log
exec tail -f /var/log/envoy/admin_access.log