# Serving Spark Model

Spark model is a model that is backed with a Spark runtime. 

## Export a Spark model

We implemented a binarizer model using pyspark API. The full code is 
available in our [GitHub repository](https://github.com/Hydrospheredata/hydro-serving-example/tree/master/examples/binarizer/bin_train.py). 
In the code, you can find the following lines describing creating 
SparkSession and training the model. The model must be embedded into 
the pipeline to be compatible with the hydrosphere. 

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

Once the model have been trained you can export the whole computational 
graph and the trained weights with `PipelineModel.save()`. In order 
to do that you have to define your model's signature:

```python
pipeline.write().overwrite().save("binarizer")
```

## Upload model

Upload the exported model to the cluster.

```sh
cd {EXPORT_DIR}   # a directory with metadata and stages directories
hs upload --runtime hydrosphere/serving-runtime-spark-2.1.2:dev
```

Now the model is uploaded to the manager service but it's not available 
for prediction inference yet. 

## Create an application

To deploy a model as microservice and provide a public endpoint you need 
to create an application. You can create it manually via UI interface, 
or by providing an application manifest.

```sh
hs apply -f - <<EOF
kind: Application
name: binarizer_app
singular:
  model: binarizer
EOF
```

You can check your applications using this command:

```sh
hs app list
```

## Inference

That's it, you can now send prediction requests. 

```sh 
curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{   "feature": 1 }' 'http://localhost/gateway/application/binarizer_app'
```
