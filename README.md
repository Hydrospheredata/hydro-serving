# Hydro-serving

This repository contains several sub-projects for serving ml models.

## Structure
* [mist-local-ml](/mist-local-ml) contains local SparkMl implementation. (Derived from Hydrosphere Mist)
* [mist-serving-gateway](/mist-serving-gateway) is a simple gateway of the whole project. For now, it's just set up Nginx.
* [ml_repository](/ml_repository) is a module that rules over all ML models, knows where they are, what they are.
* [ml_runtimes](/ml_runtimes) contains implementations for popular ML libraries. Runtime is a small server that can import user's model and provide an HTTP API to it.
* [models](/models) contains example ML models for implemented runtimes.


## How to launch demo
WIP
