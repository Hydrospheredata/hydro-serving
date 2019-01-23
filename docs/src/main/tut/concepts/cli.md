---
layout: docs
title:  "CLI"
permalink: 'cli.html'
---

# Command Line Interface

To work with Serving from the command line we've created `hs`. It allows you to _apply_ various resource configurations to the platform. 

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
$ hs cluster
``` 

To add a new cluster, use:

```sh
$ hs cluster add --name new_local --server http://localhost:16000/
```

To switch to the new cluster, use:

```sh
$ hs cluster use new_local
```

To remove the new cluster, use:

```sh
$ hs cluster rm new_local
```

<hr>

### `hs upload`

When you upload a model, the tool looks for `serving.yaml` file in the current directory. `serving.yaml` defines model's metadata and it's contract. For more information, check out [model's manifest]({{site.baseurl}}{%link how-to/write-manifests.md%}#kind-model).

<hr>

### `hs apply` 

You can apply custom resources on Serving. These resources are detected by `kind` key in the [manifest]({{site.baseurl}}{%link how-to/write-manifests.md%}) files.

- Model defines the model files, and it's contract;
- Runtime defines which runtimes to use by Serving;
- Application defines an endpoint to reach your models. 

<br>
<hr>

# What's next? 

- [Learn, how to write manifests]({{site.baseurl}}{%link how-to/write-manifests.md%});
