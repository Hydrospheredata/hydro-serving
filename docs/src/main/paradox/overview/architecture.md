# Architecture

Hydrosphere is composed out of several microservices, united to efficiently serve and monitor machine learning models in production.

![](.../architecture.png)


@@toc { depth=2 }

## Services

On a high level we can outline the following services. 

### Manager

Manager is a service component, responsible for managing Hydrosphere entities (models and applications), provisioning infrastructure resources and providing interfaces for interacting with Hydrosphere entities. 

### Gateway

Gateway is a service component responsible for handling prediction requests and routing them among model services. Gateway maps model endpoint name to a corresponding container. Whenever it receives a request it communicates with that container by gRPC protocol.

### UI

UI is a service component responsible for showing off user-friendly interface for models, applications as well as monitoring charts and profiles.

### Sonar

Sonar is a service component responsible for monitoring your models during inference phase. It allows you to evaluate how your model behaves under production load, i.e. is there a concept drift occurred in the production data (so your model needs to be retrained); how many outliers are there in the production data; how distribution of your training data is compared with the distribution of the production data?