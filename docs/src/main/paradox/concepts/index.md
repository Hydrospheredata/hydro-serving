# Concepts

@@@ index
* [Model](models.md)
* [Application](applications.md)
@@@

Hydrosphere platform can be summarized in the following diagram. It's a set of microservices that work together to achieve the final goal of managing machine learning models reliably and at scale.  

![System](../images/manager.png)

## Services 

### Manager
[Manager](https://github.com/Hydrospheredata/hydro-serving-manager) is a service component, responsible for model cataloging, building, provisioning servables and applications, working with model metadata and basically handling all the resources.

### Docker Registry
Manager creates Docker images of models which are stored in Docker registry. A model image is a read-only template with instructions for creating model applications. It can be configured to use the default Docker Registry deployed when the Hydrosphere deployment was created, or to use a self-managed Docker Registry outside of the Hydrosphere. 

### PostgreSQL
Database that contains such information as model (application) id, name, runtime type, etc. Manager creates, deletes or retrieves information about necessary model/application. 

### Gateway 
[Gateway](https://github.com/Hydrospheredata/hydro-serving-gateway) is a service component responsible for handling prediction requests and routing them among model services. Gateway maps servable name to corresponding running container. Whenever it receives a request it communicates with corresponding container by gRPC protocol.

### UI
[UI](https://github.com/Hydrospheredata/hydro-serving-ui) is a service component responsible for showing off user-friendly interfaces of uploaded models, deployed applications as well ass monitoring charts, etc. 

### Sonar
Sonar is a service component responsible for monitoring your models during inference time. It allows you to evaluate how your model behaves under production load. 

@@@ note
Currently not available in public distribution. 
@@@

## Abstractions

During the process of interaction with the platform you will encounter with the following entities: Models and Applications.

### Models

Model is a machine learning model or a processing function that consume provided inputs and produce predictions/transformations. To find out more about models refer to [this page](models.md). 

### Applications

Application is a publicly available HTTP/gRPC/Kafka endpoint to reach your models. To find out more about applications refer to [this page](applications.md). 