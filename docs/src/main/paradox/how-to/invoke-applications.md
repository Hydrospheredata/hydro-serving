# Invoke applications

Inferencing applications can be achieved using any of the ways, described below.

## Hydrosphere UI 

To send a sample request using Hydrosphere UI you should open a desired application and press the `Test` button on the upper right corder. We will generate a dummy inputs based on your model's contract and send an HTTP-request to model's endpoint. 

## HTTP API

To send an HTTP-request you should send a `POST` request to the `/gateway/application/<applicationName>` endpoint with the JSON body containing your request data composed with respect to the model's contract. 

## gRPC API

To send a gRPC request you need to create a specific client. 

Python
:   @@snip[client.py](snippets/python/invoke-applications/grpc.py)

Java
:   @@snip[client.java](snippets/java/invoke-applications/grpc.java)