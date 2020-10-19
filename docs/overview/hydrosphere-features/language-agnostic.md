# Language-Agnostic

Hydrosphere is a language-agnostic platform. You can use it with models written in any language and trained in any framework. Your ML models can come from any background, without any restrictions of your choices regarding ML model development tools.  

In Hydrosphere you operate ML models as [Runtimes](../concepts.md#runtimes), which are Docker containers with predefined dependencies and a gRPC interface to load and serve it on the platform that a model is packed into. All models that you upload to Hydrosphere must have a corresponding runtime. 

Runtimes are created by building a Docker container with dependencies required for the language that matches your model. You can [use our pre-made runtimes](../../reference/runtimes.md) or [create your own runtime](../../how-to/develop-runtimes.md). 

The Hydrosphere component responsible for building Docker images from models for deployment, storing them in the registry, versioning, and more is [Manager](../services/serving.md#manager).

