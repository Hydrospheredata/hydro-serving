### Build
Clone sources from repositories
```
#Sidecar + manager + gateway + dummy runtime 
git clone https://github.com/Hydrospheredata/hydro-serving

#ML Runtimes + ML models repository
git clone https://github.com/Hydrospheredata/hydro-serving-runtime
```

Build modules from the first repository. Change directory to `hydro-serving` and:
```
sbt compile docker
```
You will get next docker images:
* `hydro-serving/gateway` - image with gateway app - will process all HTTP requests from the client.
* `hydro-serving/manager` - image with manager app - manages all pipelines and sidecars configurations.
* `hydro-serving/dummy-runtime` - image with simple runtime.
* `hydro-serving/java` - common image for manager and gateway.

Also you will get:
* sidecar/target/hydro-serving-sidecar-install-$VERSION.sh - A self-extracting tar.

#### Available build parameters
- skipSidecarBuild - will skip sidecar build step.
```
sbt -DskipSidecarBuild=true compile docker
```
- appVersion - will specify images version.
```
sbt -DappVersion=0.0.2 compile docker
```