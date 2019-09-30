function main() {
    var client = new hello_proto.Greeter('localhost:50051',
                                         grpc.credentials.createInsecure());
    client.sayHello({name: 'you'}, function(err, response) {
      console.log('Greeting:', response.message);
    });
    client.sayHelloAgain({name: 'you'}, function(err, response) {
      console.log('Greeting:', response.message);
    });
  }