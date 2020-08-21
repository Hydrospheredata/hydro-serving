# Serving Spark model

Spark model is a model that is backed with a Spark runtime. 



## Before you start

We assume you already have a @ref[deployed](../../install/platform.md) instance of the Hydrosphere platform and a @ref[CLI](../../install/cli.md) on your local machine.

To let `hs` know where the Hydrosphere platform runs, configure a new `cluster` entity. 

```sh 
$ hs cluster add --name local --server http://localhost
$ hs cluster use local
```

## Export a Spark model

We have implemented a binarizer model using pyspark API. The full code is available in our [GitHub repository](https://github.com/Hydrospheredata/hydro-serving-example/tree/master/examples/binarizer/bin_train.py). In the code you can find the following lines describing SparkSession creation and model training. The model must be embedded into the pipeline to be compatible with the Hydrosphere. 

```python
...
spark = SparkSession\
        .builder\
        .appName("BinarizerExample")\
        .getOrCreate()
...
continuousDataFrame = spark.createDataFrame([(4)], [ "feature"])
binarizer = Binarizer(threshold=5, inputCol="feature", outputCol="binarized_feature")
pipeline = Pipeline(stages=[binarizer])
pipeline = pipeline.fit(continuousDataFrame)
...
```

Once the model has been trained, you can export the whole computational graph and the trained weights with `PipelineModel.save()`. In order to do that, you have to define your model's signature:

```python
pipeline.write().overwrite().save("binarizer")
```

## Uploading artifacts

Upload the exported model to the cluster.


```sh
$ cd {EXPORT_DIR}   # a directory with metadata and stages directories
$ hs upload --runtime hydrosphere/serving-runtime-spark-2.1.2:$released_version$
```


Now the model is uploaded to the manager service, but it is not available 
for prediction yet. 

## Creating an application

To deploy a model as a microservice, you need to create an application. You can create it manually via Hydrosphere UI, or by providing an application manifest.

```sh
$ hs apply -f - <<EOF
kind: Application
name: binarizer_app
singular:
  model: binarizer:1
EOF
```

## Prediction

That's it, you can now send prediction requests. 

```sh 
$ curl --request POST --header 'Content-Type: application/json' --header 'Accept: application/json' \
--data '{   "feature": 1 }' 'http://localhost/gateway/application/binarizer_app'
```
