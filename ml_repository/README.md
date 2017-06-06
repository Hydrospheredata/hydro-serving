# Hydro-serving repository

This project aims to satisfy the need of models structuring, and provides unified API for retrieving those models.

## How it works?

1. User specifies locations of models (datasources) in configuration file [config/repository.conf](config/repository.conf).
2. Repository identifies them, and creates a `SourceWatcher` actor for each datasource.
SourceWatcher knows the environment of datasource and subscribes to it's updates, if possible.
3. For each update, `SourceWatcher` collects data of changes via `RuntimeDispatcher`, that recognizes the Runtime of given models, and reads model's given metadata.
4. After that, it triggers the reindexing of `Repository` actor, that serves as a storage of metadata.

User is able to query the repository via HTTP API.

* `GET /metadata/<model_name>` returns metadata of specified model.
* `GET /files/<model_name>` returns a list of model's files.
* `GET /download/<model_name>/<file>` downloads the given file of specified model. Thus, repository also acts as a proxy between Runtime and actually the storage of models.
