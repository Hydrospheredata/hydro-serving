### Build
Clone sources from repositories
``` 
git clone https://github.com/Hydrospheredata/hydro-serving
```

Build modules from repository. Change directory to `hydro-serving` and:
```
sbt compile docker
```

Run tests:
```
sbt test
```

Run integration tests:
```
sbt it:testOnly
```