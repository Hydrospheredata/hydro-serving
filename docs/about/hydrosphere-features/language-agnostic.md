# Language-Agnostic

Hydrosphere is a language-agnostic platform. You can use it with models written in any language and trained in any framework. Your ML models can come from any background, without restrictions of your choices regarding ML model development tools.  

In Hydrosphere you operate ML models as [Runtimes](../concepts.md#runtimes), which are Docker containers packed with predefined dependencies and gRPC interfaces for loading and serving them on the platform with a model inside. All models that you upload to Hydrosphere must have the corresponding runtimes. 

Runtimes are created by building a Docker container with dependencies required for the language that matches your model. You can either [use our pre-made runtimes](../../resources/reference/runtimes.md) or [create your own runtime](../../quickstart/how-to/develop-runtimes.md). 

The Hydrosphere component responsible for building Docker images from models for deployment, storing them in the registry, versioning, and more is [Manager](../services/serving.md#manager).

