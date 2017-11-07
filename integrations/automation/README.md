# What is this?

This folder contains automation scripts to bootstrap demo environment using docker-compose

## Requirements:
- Docker
- Git installed and configured using ssh key

## Configs

Config file is located at hydro-serving/integrations/automation/.env.
It contains microservices version. You can update it manually to start microservices with different versions.

## Installation

To bootstrap environment just clone this repo end execute script:

```
git clone git@github.com:Hydrospheredata/hydro-serving.git
cd hydro-serving/integrations/automation/
./start_all.sh
```

After that you can work with your env.


To destroy everything execute:

```
cd hydro-serving/integrations/automation/
destroy_all.sh
```



