# Runtimes

__Runtime__ is a Docker image with a predefined infrastructure. It 
implements a set of specific methods that are used as endpoints to 
the model. It's responsible for running user-defined models. When you 
create a new application you also have to provide a corresponding runtime 
to each models' instances.

We've already implemented a few runtimes which you can use in your own 
projects. They are all open-source and you can look up code if you need. 

## Python

Code is available on [Github](https://github.com/Hydrospheredata/hydro-serving-python).

| Version | Image | Link |
| ------- | ----- | ---- |
| 3.7 | hydrosphere/serving-runtime-python-3.7:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-python-3.7) |
| 3.6 | hydrosphere/serving-runtime-python-3.6:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-python-3.6) |
| 3.5 | hydrosphere/serving-runtime-python-3.5:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-python-3.5) |

## Tensorflow

Code is available on [GitHub](https://github.com/Hydrospheredata/hydro-serving-tensorflow).

| Version | Image | Link |
| ------- | ----- | ---- |
| 1.13.1 | hydrosphere/serving-runtime-tensorflow-1.13.1:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-tensorflow-1.13.1) |
| 1.12.0 | hydrosphere/serving-runtime-tensorflow-1.12.0:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-tensorflow-1.12.0) |
| 1.11.0 | hydrosphere/serving-runtime-tensorflow-1.11.0:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-tensorflow-1.11.0) |
| 1.10.0 | hydrosphere/serving-runtime-tensorflow-1.10.0:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-tensorflow-1.10.0) |
| 1.9.0 | hydrosphere/serving-runtime-tensorflow-1.9.0:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-tensorflow-1.9.0) |
| 1.8.0 | hydrosphere/serving-runtime-tensorflow-1.8.0:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-tensorflow-1.8.0) |
| 1.7.0 | hydrosphere/serving-runtime-tensorflow-1.7.0:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-tensorflow-1.7.0) |

## Spark

Code is available on [GitHub](https://github.com/Hydrospheredata/hydro-serving-spark).

| Version | Image | Link |
| ------- | ----- | ---- |
| 2.2.0 | hydrosphere/serving-runtime-spark-2.2.0:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-spark-2.2.0) |
| 2.1.2 | hydrosphere/serving-runtime-spark-2.1.2:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-spark-2.1.2) |
| 2.0.2 | hydrosphere/serving-runtime-spark-2.0.2:$project.released_version$ | [Docker Hub](https://hub.docker.com/r/hydrosphere/serving-runtime-spark-2.0.2) |


@@@ note
If you are using a framework for which runtime isn't implemented yet, you 
can open an [issue][github-serving-new-issue] in our Github.
@@@

[github-serving-new-issue]: https://github.com/Hydrospheredata/hydro-serving/issues/new