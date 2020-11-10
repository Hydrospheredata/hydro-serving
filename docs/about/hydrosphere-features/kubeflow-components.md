# Kubeflow Components

Hydrosphere Serving Components for Kubeflow Pipelines provide integration between Hydrosphere model serving benefits and [Kubeflow](https://kubeflow.org) orchestration capabilities. This allows launching training jobs as well as serving the same models in Kubernetes in a single pipeline.

![](../../.gitbook/assets/hydrosphere_and_kubeflow%20%281%29%20%284%29.png)

You can find examples of sample pipelines [here](https://github.com/kubeflow/pipelines/samples/contrib/hydrosphere-samples).

## Serving components

### **Deploy**

The Deploy component allows you to upload a model, trained in a Kubeflow pipelines workflow to a Hydrosphere platform.

For more information, check [Hydrosphere Deploy Kubeflow Component](https://github.com/kubeflow/pipelines/tree/master/components/hydrosphere/serving/deploy_op)

### **Release**

The Release component allows you to create an Application from a model previously uploaded to Hydrosphere platform. This application will be capable of serving prediction requests by HTTP or gRPC.

For more information, check [Hydrosphere Release Kubeflow Component](https://github.com/kubeflow/pipelines/tree/master/components/hydrosphere/serving/release_op)

