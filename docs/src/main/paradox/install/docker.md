# Docker Installation

## Prerequisites

- [Docker 18.0+][docker-install]
- [Docker Compose 1.23+][docker-compose-install]


## Installation 

The following installation options are available. 

### From release

Download and unpack the latest released version from 
[releases page](https://github.com/Hydrospheredata/hydro-serving/releases). 


### From source

Clone serving repository.

```sh
git clone https://github.com/Hydrospheredata/hydro-serving
```

Deploy components using docker-compose. 

```sh
cd hydro-serving/
docker-compose up -d
```

To check that everything works fine, open [http://localhost/](http://localhost/).
By default UI is available at port __80__.

[docker-install]: https://docs.docker.com/install/
[docker-compose-install]: https://docs.docker.com/compose/install/#install-compose
