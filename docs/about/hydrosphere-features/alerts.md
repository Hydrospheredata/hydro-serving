# Alerts

## Overview

\*\*\*\*[**Sonar**](../services/monitoring.md#sonar) sends data about any failed health checks of live production models and applications to **Prometheus AlertManager**. Once a user deploys a model to production, adds training data and starts sending production requests, these requests start getting checked by Sonar. If Sonar detects an anomaly \(for example, a data check failed, or a metric value exceeded the threshold\), AlertManager sends an appropriate alert.

Users can manage alerts by setting up AlertManager for Prometheus on Kubernetes. This can be helpful when you have models that you get too many alerts from and need to filter, group, or partly silence them. AlertManager can take care of grouping, inhibition, silencing of alerts, and routing them to the receiver integration of your choice. To configure alerts, modify the `prometheus-am-configmap-<release_name>` ConfigMap.

For more information about Prometheus AlertManager please refer to its [official documentation](https://prometheus.io/docs/alerting/latest/alertmanager/).

