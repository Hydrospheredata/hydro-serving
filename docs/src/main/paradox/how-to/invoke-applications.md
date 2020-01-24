# Invoke applications

You can use on the following APIs to send prediction requests to your applications:

## Hydrosphere UI 

You can send a test request to the application from Hydrosphere UI interface. Just open an application and press `Test` button. We will generate a dummy inputs based on your model's contract and send an HTTP-request to model's endpoint. 

## HTTP API

You can reach your application with an HTTP-request. Send a `POST` request to the `/gateway/application/<applicationName>` endpoint with the JSON body containing your request data composed with respect to model's contract. 

## gRPC API

To send a gRPC request you need to create a specific client. 

Python
:   @@snip[client.py](snippets/python/invoke-applications/grpc.py)

Java
:   @@snip[client.java](snippets/java/invoke-applications/grpc.java)