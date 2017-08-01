### Build sidecar
- `./side_car_package.sh $SOME_VERSION` - will create a self-extracting package. Package can be found here `target/hydro-serving-sidecar-install-$SOME_VERSION.sh`.
- `./java_with_sidecar.sh $SOME_VERSION` - will create docker image `hydro-serving/java:$SOME_VERSION` with installed java and sidecar from previous script.
- `./dummy_runtime.sh $SOME_VERSION` - will create simple runtime, this script depends on side_car_package.sh results.
- `./build_all.sh $SOME_VERSION` - aggregates all scripts.


### Create your own Runtime

