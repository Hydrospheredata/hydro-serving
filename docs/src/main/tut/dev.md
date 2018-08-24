---
layout: docs
title:  "For Developers"
permalink: 'dev.html'
---

# For Developers

This section is dedicated to the developers, who want to improve ML Lambda. 

For the development you'll probably need a working example, so first clone serving examples.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving-example 
```

To set up a development environment clone ML Lambda repository.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving
```

Run PostgreSQL, Sidecar and UI containers.

```sh
$ cd hydro-serving
$ sbt manager/devRun
```

Removing containers is done via: 

```sh
$ sbt manager/cleanDockerEnv
```

To upload serving examples to ML Lambda, you'll need `hs` cli-tool. 

```sh
$ pip install hs
```

Now, go to the `claims` model example and upload it.

```sh 
$ cd ../hydro-serving-example/model/claims
$ hs upload
```

You can see the uploaded model from [http://localhost/][http://localhost/]. 