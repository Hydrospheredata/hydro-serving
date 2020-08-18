# Architecture

Hydrosphere is composed out of several microservices, united to efficiently serve and monitor machine learning models in production.

![](./images/architecture.png)




## Services

On a high level we can outline the following services. 

### Manager

Manager manages Hydrosphere entities (models and applications), provisions infrastructure resources and provides interfaces for interacting with Hydrosphere entities. 

### Gateway

Gateway handles prediction requests and routing them among model services. Gateway maps model endpoint name to a corresponding container. Whenever it receives a request it communicates with that container by gRPC protocol.

### UI

UI displays user-friendly interface for models, applications as well as monitoring charts and profiles.

### Sonar

Sonar monitors your models during inference phase. User can use sonar to evaluate how a model behaves under production load. 
For instance you can:
1. check if a concept drift occurred in the production data (so your model needs to be retrained)
2. see how many outliers are there in the production data
3. see how distribution of your training data is compared with the distribution of the production data?

#### S3 Storage

**Sonar** uses [S3](https://aws.amazon.com/s3/) to store model training data and production requests data. 
