# mist-serving-gateway

```
#Clone repository
git clone https://github.com/Zajs/mist-serving-gateway.git   mist-serving-gateway
cd mist-serving-gateway

#Build images
./build.sh
#Start containers
docker-compose up
```
* Runtime url: http://localhost:8081/somethink
* Consul http://localhost:8500/ui/#/dc1/services
* Serving Gateway: http://localhost:8080/mist-runtime-type-name/somethink