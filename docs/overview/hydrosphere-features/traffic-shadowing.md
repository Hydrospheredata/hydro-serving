# Traffic Shadowing

## A/B Deployment

Hydrosphere users can use multiple [model versions](../concepts.md#models-and-model-versions) inside of the same [Application](../concepts.md#applications) stage. Hydrosphere shadows traffic to all model versions inside of an application stage.  

Users can specify the likelihood that a model output will be selected as an application stage output by using a `weight` argument.

![Traffic is shadowed to all versions, but only v2 and v3 return output](../../.gitbook/assets/ab-deployment-and-traffic-shadowing-1-.png)

## Traffic Shadowing

Hydrosphere shadows traffic to all model versions inside of an application stage. 

If you want to shadow your traffic between model versions without producing output from them simply set `weight` parameter to `0`. This way your model version will receive all incoming traffic, but it's output will never be chosen as an output of an application stage.

