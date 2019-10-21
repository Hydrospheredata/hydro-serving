# For developers

This section is dedicated to those of you, who want to improve Hydrosphere. 

## Prerequisites

To set up a development environment clone Serving repository.

```sh
git clone https://github.com/Hydrospheredata/hydro-serving
```

For the development you'll probably need working model examples.

```sh
git clone https://github.com/Hydrospheredata/hydro-serving-example 
```

To upload serving examples to Serving, you'll need `hs` cli-tool. 

```sh
pip install hs
```

Add new local cluster.

```sh
hs cluster add --name local --server http://localhost/
```

## Development

Run PostgreSQL, Sidecar and UI containers.

```sh
cd hydro-serving
sbt manager/devRun
```

Removing containers is done with: 

```sh
sbt manager/cleanDockerEnv
```

## Model uploading 

Go to the `claims` model example and upload it.

```sh 
cd hydro-serving-example/model/claims
hs upload
```

You can see the uploaded model from [http://localhost/](http://localhost/). 