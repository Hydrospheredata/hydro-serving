---
layout: docs
title:  "Runtimes"
permalink: 'runtimes.html'
---

# Runtimes

__Runtime__ is a Docker image with a predefined infrastructure. It implements a set of specific methods that are used as an endpoints to the model. It's responsible for running user-defined models. When you create a new application you also have to provide a corresponing runtime to each models' instances.

We've already implemented a few runtimes which you can use in your own projects. They are all open-source and you can look up code if you need. 

| Framework | Runtime | Links |
| --------- | ------- | ----- |
| Python | hydrosphere/serving-runtime-python | [Docker Hub][docker-hub-python]<br>[Github][github-serving-python]|
| TensorFlow | hydrosphere/serving-runtime-tensorflow | [Docker Hub][docker-hub-tensorflow]<br>[Github][github-serving-tensorflow] |
| Spark | hydrosphere/serving-runtime-spark | [Docker Hub][docker-hub-spark]<br>[Github][github-serving-spark] |

<br>

_Note: If you are using a framework for which runtime isn't implemented yet, you can open an [issue][github-serving-new-issue] in our Github._

<hr>

# What's Next?

- [Learn, how to develop a runtime;]({{site.baseurl}}{%link how-to/develop-runtime.md%})


[docker-hub-python]: https://hub.docker.com/r/hydrosphere/serving-runtime-python/
[docker-hub-spark]: https://hub.docker.com/r/hydrosphere/serving-runtime-spark/
[docker-hub-tensorflow]: https://hub.docker.com/r/hydrosphere/serving-runtime-tensorflow/
[github-serving-new-issue]: https://github.com/Hydrospheredata/hydro-serving/issues/new
[github-serving-python]: https://github.com/Hydrospheredata/hydro-serving-python
[github-serving-tensorflow]: https://github.com/Hydrospheredata/hydro-serving-tensorflow
[github-serving-spark]: https://github.com/Hydrospheredata/hydro-serving-spark