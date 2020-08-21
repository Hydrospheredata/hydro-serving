# A/B Deployment and Traffic Shadowing

## A/B Deployment

TODO

![](../../.gitbook/assets/ab-deployment-and-traffic-shadowing-1-.png)

## Traffic Shadowing

Hydrosphere shadows traffic between all model versions inside of an application stage. 

If you want to shadow your traffic between model versions without producing output from them simply set `weight` parameter to `0`. This way your model version will receive all incoming traffic, but it's output will never be chosen as an output of an application stage.

