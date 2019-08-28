# `hs` CLI tool

`hs` is a ClI interface to serving cluster, that aggregates API to various services.

## Installation

`pip install hs`

@@@ note
The package is only for python3
@@@

## Commands

@@@ note
You can add `--help` parameter for any command for additonal info.
@@@

### `hs cluster`

Cluster is a space, where you deploy your models. Simply put, it points where to apply your configurations and where to upload your models. It stores all the clusters in `~/.hs-home/config.yaml` file.

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

When you upload a model, the tool looks for `serving.yaml` file in the current directory. `serving.yaml` defines model's metadata and it's contract. For more information, check out [model's manifest](../reference/manifests.html#kind-model).

### `hs apply` 

You can apply custom resources on Serving. These resources are detected by `kind` key in the [manifest](../reference/manifests.html) files.

- Model defines the model files, and it's contract;
- Application defines an endpoint to reach your models. 

### `hs profile push`

### `hs app list`

### `hs app rm`

### `hs model list`

### `hs model rm`
