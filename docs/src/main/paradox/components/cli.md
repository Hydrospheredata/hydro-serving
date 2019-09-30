# CLI

`hs` is a CLI interface to Hydrosphere platform, that aggregates API 
to various services.

## Installation

`pip install hs`

@@@ note
The package is only for Python 3.
@@@

## Commands

@@@ note
You can add `--help` parameter for any command for additional info.
@@@

### `hs cluster`

Cluster is a space, where you deploy your models. Simply put, it points 
where to apply your configurations and where to upload your models. It 
stores all the clusters in `~/.hs-home/config.yaml` file.

```yaml
# Example configuration

kind: Config
clusters:
  - cluster: 
      server: "http://127.0.0.1"
    name: local
current-cluster: local
```

To look up, which cluster you're using, execute:

```sh
hs cluster
``` 

To add a new cluster, use:

```sh
hs cluster add --name new_local --server http://localhost:80/
```

To switch to the new cluster, use:

```sh
hs cluster use new_local
```

To remove the new cluster, use:

```sh
hs cluster rm new_local
```

### `hs upload`

When you upload a model, the tool looks for `serving.yaml` file in the 
current directory. `serving.yaml` defines model's metadata and it's 
contract. For more information, check out 
[model's manifest](../reference/manifests.html#kind-model).

### `hs apply` 

This command has similar functionality as `hs upload` but allows you not 
only to deploy models, but also create applications, define pipeline and 
stages inside application. Types of resource to apply are detected by 
`kind` key in the  [manifest](../reference/manifests.html) files.

- Model defines the model files, and it's contract;
- Application defines an endpoint to reach your models. 

Example of application configuration:

```yaml
kind: Application
name: sample-claims-app
pipeline:
  - model: claims-preprocessing:1
  -  modelservices:
      - model: claims-model:1
        weight: 80
      - model: claims-model-old:2
        weight: 20
```

### `hs profile`

Commands: 
`push` - uploads training dataset to compute its profiles
`status` - shows profiling status for given model

### `hs app `

This command provides information about available apps

Commands:

`list` - lists all existing apps

`rm` - removes certain app


### `hs model`

This command provides information about available models

Commands:

`list` - lists all existing models

`rm` - removes certain model
