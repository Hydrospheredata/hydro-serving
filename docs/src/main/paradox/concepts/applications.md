# Applications 

__Application__ is a publicly available endpoint to reach your models. 
It allows you to use your most recent deployed production models via 
HTTP-requests, gRPC API calls or configure it as a part of Kafka streams. 

When configuring applications you have 2 options:

1. __Singular__ application is an option if you want to use just one of 
your models. In that case your model handles all necessary transformations 
and data cleaning itself and produces only the desired result. 

2. __Pipeline__ application is an option if you want to create pipelines 
that will let data flow through different models, perform pre/post 
processing operations, etc.
