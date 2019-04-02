# Serving Tensorflow Model

Deploying Tensorflow models does not require any additional manifest writings for model uploading since Serving can automatically infer models' signatures. Running Tensorflow models can be done with `hydrosphere/serving-runtime-tensorflow` runtime. The only thing you have to worry about is how to properly export your model with `tf.saved_model`.

Suppose we already have a Tensorflow model, that recognizes handwritten digits. We will cover serving for Tensorflow [Basic API](#basic-api) and [Estimator API](#estimator-api). 

<br>

## Basic API

When serving a Tensorflow model the only thing you have to do is to correctly identify and prepare input and output tensors. We've implemented a linear model using low-level Tensorflow API. The full code is available in our [GitHub repository](https://github.com/Hydrospheredata/hydro-serving-example/blob/master/models/mnist/basic-api.py). In the code you can find the following lines which are the input and output tensors. 

```python
...
img, label = iterator.get_next()    # input image and label
...
pred = tf.nn.softmax(logits)        # output prediction
...
```

Once the model have been trained you can export the whole computational graph and the trained weights with `tf.saved_model`. You have to define a signature definition which declares a computation supported on the graph.  

```python
# Save model
signature_map = {
    "infer": tf.saved_model.signature_def_utils.predict_signature_def(
        inputs={"img": img}, outputs={"pred": pred})
}
```

After that create a `SavedModelBuilder` object and save the model under desired directory.

```python
builder = tf.saved_model.builder.SavedModelBuilder(export_dir)
builder.add_meta_graph_and_variables(
    sess=sess,                                          # session, where the graph was initialized
    tags=[tf.saved_model.tag_constants.SERVING], 
    signature_def_map=signature_map)
builder.save()
```

### Serving the model

Upload the exported model to Serving. 

```sh
$ cd {EXPORT_DIR}
$ hs upload --name mnist
```

Now the model is uploaded to the serving service but does not yet available for the invocation. Create an application to declare an endpoint to your model. You can create it manually via UI interface, or by providing an application manifest. To do it with web interface, open your `http://<host>/` where Serving has been deployed, open Applications page and create a new app which will use `mnist` model. Or by manifest:

```yaml
# application.yaml 

kind: Application
name: mnist_app
singular:
  model: mnist:1
  runtime: hydrosphere/serving-runtime-tensorflow:1.7.0-latest
```

```sh
$ hs apply -f application.yaml
```

That's it, you can now infer predictions. 

```sh 
$ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{ "imgs": [ [ [ 1, 1, 1, ... 1, 1, 1 ] ] ] }' 'https://<host>/gateway/applications/mnist_app/infer'
```

<br>

## Estimator API

Saving model written with Estimator API does not differ much from Basic API. Likewise you have to declare a signature map and export the model with `tf.saved_model`. All code for this model available in our [GitHub repository](https://github.com/Hydrospheredata/hydro-serving-example/blob/master/models/mnist/estimator-api.py).

```python
...
imgs = tf.feature_column.numeric_column("imgs", shape=(784,))
estimator = tf.estimator.DNNClassifier(
    hidden_units=[256, 64],
    feature_columns=[imgs],
    n_classes=10,
    model_dir='models/mnist')
...
```

Instead of declaring a signature map as in the Basic API example, you need to declare a `build_raw_serving_input_receiver_fn` function and pass it to the `export_estimator`. `imgs` tensor in the `input_receiver_fn` should be similar to the `tf.feature_column` which is expected by the estimator. 

```python
serving_input_receiver_fn = tf.estimator.export.build_raw_serving_input_receiver_fn({
    "imgs": tf.placeholder(tf.float32, shape=(None, 784))})
estimator.export_savedmodel(export_dir, serving_input_receiver_fn)
```

### Serving the model

Upload the exported model to Serving. 

```sh 
$ cd ${EXPORT_DIR}/{TIMESTAMP}
$ hs upload --name mnist
```

The rest is the same as with the [Basic API example]({{site.baseurl}}{%link tutorials/tensorflow.md%}#serving-the-model). Create an application and infer predictions. 

```sh 
$ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{ "imgs": [ [ [ 1, 1, 1, ... 1, 1, 1 ] ] ] }' 'https://<host>/gateway/applications/mnist_app/predict'
```
