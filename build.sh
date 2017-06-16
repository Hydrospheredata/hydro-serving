echo "Building dependencies and Docker images for demo..."

echo "Building envoy and other dependencies..."
cd envoy
mvn clean install
cd ../

echo "Mist Local ML library..."
cd mist-local-ml
sbt publishLocal
cd ../

echo "Serving gateway..."
docker build --no-cache -t mist-serving-gateway ./mist-serving-gateway/mist-serving-gateway

echo "ML Repository..."
cd ml_repository
sbt assembly
docker build --no-cache -t mist-ml-repository .
cd ../


echo "Runtimes:"
cd ml_runtimes

echo "Spark Local ML..."
cd localml-spark
sbt assembly
docker build --no-cache -t mist-runtime-sparklocal .
cd ../

echo "Scikit..."
cd scikit
docker build -t mist-envoy-alpine-python-machinelearning -f Dockerfile-alpine-python-machinelearning .
docker build --no-cache -t mist-runtime-scikit .
cd ../

echo "Custom Scikit..."
cd custom_scikit
docker build -t mist-envoy-alpine-python-machinelearning -f Dockerfile-alpine-python-machinelearning .
docker build --no-cache -t mist-runtime-customscikit .
cd ../

cd ../

echo "Build complete. Images are ready to run."
