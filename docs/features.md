#### Repository
0. Add repository + build model to watcher
1. Watch ML model storage (s3, hdfs) + build runtime
2. Watch Docker registry
3. Watch source code tags in GIT + compile and build runtime
4. Store/Update input/output runtime params
5. List available runtime + build information
 
runtime name format: {group}/{modelName}:version
- group - model type mist-sparkml,mist-tenserflow,custom-name 
- modelName - some model name
- version - full hash in docker registry 

#### Manager
0. Create runtime deploy configuration (instance count + env variables)
1. Deploy runtime to cluster (using Docker Swarm, ECS, Kubernetes, etc...)
2. List running runtime 
3. Provide urls to metrics and tracing information
4. Provide configuration for **Sidecar** - routes,services,clusters

#### Gateway
1. Authorize client
2. Map requestUrl to pipeline
3. Wait pipeline results

#### Sidecar
1. Register in service discovery
2. Send metrics
3. Send tracing information
4. Route messages based on pipelines from **Manager**
5. Interact with ML serving process using RestAPI or NamedPipes
6. CircuitBreak + HealthCheck