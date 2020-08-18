# Manager

{% hint style="info" %} Manager is an open-source service available
[here](https://github.com/Hydrospheredata/hydro-serving-manager) as a
part of
[hydro-serving](https://github.com/Hydrospheredata/hydro-serving)
project. {% endhint %}

Manager is responsible for:

* Building a Docker Image from your ML model for future deployment
* Storing these images inside a Docker Registry deployed alongisde with
  manager service
* Versioning these images as Model Versions
* Creating running instances of these Model Versions called Servables
  inside Kubernetes cluster
* Combining multiple Model Versions into a linear graph with a single
  endpoint called Application


![](.../serving_screenshot.png)
