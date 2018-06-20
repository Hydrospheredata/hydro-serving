---
layout: post
title:  "Trouble Shooting"
date:   2018-06-15
permalink: 'troubleshooting.html'
---

Down below listed some workarounds that may help you to resolve your issues. If
those do not help, you can ask a question via [Gitter][gitter].

### Connection Refused Error

If you encounter a connection error to your server, while working with ML Lambda
in your local environment:

```python
Failed to establish a new connection: [Errno 61] Connection refused.
```

Try to resolve this by stopping your docker instance and rebuilding existing 
containters:

```sh
$ docker stop $(docker ps -aq)
$ docker rm $(docker ps -aq)
$ docker-compose up
```


[gitter]: https://gitter.im/Hydrospheredata/hydro-serving?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
