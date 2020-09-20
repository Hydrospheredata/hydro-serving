# Alerts



{% hint style="info" %}
Hydrosphere Alerts about failed health checks and other issues with models are not available as an open-source solution. If you are interested in this component you can contact us via [Gitter](https://gitter.im/Hydrospheredata/hydro-serving) or our [website](https://hydrosphere.io).
{% endhint %}

### Overview

\*\*\*\*[**Sonar**](../services/monitoring.md#sonar) sends data about any failed health checks of live production models and applications to **Prometheus AlertManager**. Once a user deploys a model to production, adds training data, and starts sending production requests, these requests start getting checked by Sonar. If Sonar detects an anomaly \(for example, a health check failed, or a metric value exceeded the threshold\), AlertManager sends an appropriate alert.   

Users can manage alerts by setting up AlertManager for Prometheus on Kubernetes. This can be helpful when you have models that you get too many alerts from and need to filter, group, or partly silence them. AlertManager can take care of grouping, inhibition, silencing of alerts, and routing them to the receiver integration of your choice. To configure alerts, modify the `prometheus-am-configmap-<release_name>` ConfigMap. 

For more information about Prometheus AlertManager please refer to its [official documentation](https://prometheus.io/docs/alerting/latest/alertmanager/). 

### Grouping

This feature of AlertManager allows grouping of multiple similar alerts into a single notification. **Example:** you have many instances of a service running in your cluster. A network partition occurs, and half of them can no longer reach the database. Without grouping, hundreds of alerts are sent as a result. With grouping configured by cluster and `alertname`, you get all the necessary information about each affected instance but grouped under a single alert. 

**Configure in:** AlertManager's configuration file. 

### Inhibition

This feature of AlertManager allows suppressing notifications for certain alerts if certain other alerts are already firing. **Example:** if an alert fires telling that the entire cluster is unreachable, you won't get any resulting alerts about this cluster until it's back to normal. 

**Configure in:** AlertManager's configuration file.  

### Silences

This feature of AlertManager allows muting alerts for a given time. Alerts are getting silenced based on matchers. They get checked for matching these requirements for silencing, and if they do, they get silenced and no notifications will be sent out for matched alerts. 

**Configure in:** AlertManager's web interface. 



