## Application

Application is a public endpoint for models.

There are currently two separate configurations of an application:

1. Pipeline. 
2. Single model with runtime

### Pipeline
Pipeline consists of many stages. Each stage is defined by signature name, attached to it, and services.
Each service is a combination of model version, runtime, and weight (for A/B tests).

When user submits an application creation request, manager infers a contract for possible application.
If inferred contract is valid, non-ambiguous and correct, then application is created.

### Single model
User can deploy a builded model as an application. In this case, application inherits contract from the model.