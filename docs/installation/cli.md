# CLI

Hydrosphere CLI, or`hs`,  is a command-line interface designed to work with the Hydrosphere platform.

Source code: [https://github.com/Hydrospheredata/hydro-serving-cli](https://github.com/Hydrospheredata/hydro-serving-cli)  
PyPI: [https://pypi.org/project/hs/](https://pypi.org/project/hs/)

## Installation

Use pip to install `hs`:

```bash
pip install hs=={{ releasedVersion }}
```

Check the installation:

```bash
hs --version
```

## Usage

### `hs cluster`

This command lets you operate cluster instances. A cluster points to your Hydrosphere instance. You can use this command to work with different Hydrosphere instances.

See `hs cluster --help` for more information.

### `hs upload`

This command lets you upload models to the Hydrosphere platform. During the upload, `hs` looks for a `serving.yaml` file in the current directory. This file **must** contain a definition of the model \([example](../how-to/write-definitions.md#kind-model)\).

See `hs upload --help` for more information.

### `hs apply`

This command is an extended version of the `hs upload` command, which also allows you to operate applications and host selector resources.

See `hs apply --help` for more information.

### `hs profile`

This command lets you upload your training data to build profiles.

* `$ hs profile push` - upload training data to compute its profiles. 
* `$ hs profile status` - show profiling status for a given model.

See `hs profile --help` for more information.

### `hs app`

This command provides information about available applications.

* `$ hs app list` - list all existing applications.
* `$ hs app rm` - remove a certain application.

See `hs app --help` - for more information.

### `hs model`

This command provides information about available models.

* `$ hs model list` - list all existing models.
* `$ hs model rm` - remove a certain model.

See `hs model --help` for more information.

