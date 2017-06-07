# ML Runtime

ML runtime is a small server that can import pre-trained user model and serve
it via unified HTTP API.

## HTTP API
For now, the main access point of runtimes is
`POST /<model_name>` method.

### Request body example

| sepal length (cm) | sepal width (cm) | petal length (cm) | petal width (cm) | features  | color |
|-------------------|------------------|-------------------|------------------|-----------|-------|
|        5.0        |        3.0       |        1.6        |        0.2       |[1,2,3,4,5]|   12  |
|        5.9        |        3.0       |        5.1        |        1.8       |[8,0,5,1,7]|   1   |

This dataframe can be represented in row-wise column list:

```
[
  {
    "sepal length (cm)": 5.0,
    "sepal width (cm)": 3.0,
    "petal length (cm)": 1.6,
    "petal width (cm)": 0.2,
    "features": [ 1, 2, 3, 4, 5],
    "color": 12,
  },
  {
    "sepal length (cm)": 5.9,
    "sepal width (cm)": 3.0,
    "petal length (cm)": 5.1,
    "petal width (cm)": 1.8,
    "features": [8, 0, 5, 1, 7],
    "color": 1,
  }
]
```

That is unified input structure for all runtimes.

## Model retrieval
Every runtime knows about ML repository.
Repository address is specified by `ML_REPO_ADDR` and `ML_REPO_PORT` environment variables.
When request to serve a new model is received, runtime downloads model and it's metadata from repository.

## HTTP parameters
Runtime HTTP server parameters are set up with `SERVE_ADDR` and `SERVE_PORT` environment variables. Their defaults are `0.0.0.0` and `8080` respectively.

## Implemented runtimes
* [sklearn](scikit/)
* [spark-ml (local imlpementation)](localml-spark/)
