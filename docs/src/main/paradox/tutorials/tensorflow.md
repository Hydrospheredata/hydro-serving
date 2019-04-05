# Serving Tensorflow Model

## Export a TensorFlow model

### SavedModelBuilder

To export a Tensorflow model correctly you have to identify and prepare input and output tensors.
We implemented a linear model using low-level Tensorflow API. The full code is available in our 
[GitHub repository](https://github.com/Hydrospheredata/hydro-serving-example/blob/master/models/mnist/basic-api.py). 
In the code you can find the following lines describing input and output tensors. 

```python
...
img, label = iterator.get_next()    # input image and label
...
pred = tf.nn.softmax(logits)        # output prediction
...
```

Once the model have been trained you can export the whole computational graph 
and the trained weights with `tf.saved_model`. In order to do that you have 
to define your model's signature:

```python
signature_map = {
    "infer": tf.saved_model.signature_def_utils.predict_signature_def(
        inputs={"img": img}, outputs={"pred": pred})
}
```

The next step is to create a `SavedModelBuilder` object and save the model under desired directory.

```python
builder = tf.saved_model.builder.SavedModelBuilder(export_dir)
builder.add_meta_graph_and_variables(
    sess=sess,                                          # session, where the graph was initialized
    tags=[tf.saved_model.tag_constants.SERVING],        # tag your graph as servable using this constant
    signature_def_map=signature_map)
builder.save()
```

### Estimator API

Full code for this model available in our [GitHub repository](https://github.com/Hydrospheredata/hydro-serving-example/blob/master/models/mnist/estimator-api.py).

Assume that we defined an estimator:
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

In order to export it we need to use `build_raw_serving_input_receiver_fn`
function and pass the result to the `export_estimator` method. 
`imgs` tensor in the `input_receiver_fn` should be similar 
to the `tf.feature_column` that is expected by the estimator. 

```python
serving_input_receiver_fn = tf.estimator.export.build_raw_serving_input_receiver_fn({
    "imgs": tf.placeholder(tf.float32, shape=(None, 784))
})
estimator.export_savedmodel(export_dir, serving_input_receiver_fn)
```

## Upload model

Upload the exported model to the cluster.

```sh
cd {EXPORT_DIR}   # a directory with saved_model.pb file
hs upload --name mnist --runtime hydrosphere/serving-runtime-tensorflow-{tensorflow version}:dev
```

Now the model is uploaded to the manager service but it's not available for 
prediction inference yet. 

## Create an application

To deploy a model as microservice and provide a public endoint you need to create an application.
Applications can be created using web-UI or CLI.
For the sake of simplicity we will create it using CLI:
```sh
hs apply -f - <<EOF
kind: Application
name: mnist_app
singular:
  model: mnist:1
EOF
```
You can check your applications using this command:
```sh
hs app list
```

## Send request

That's it, you can now send prediction requests. 

```sh 
curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{ "imgs": [ [ [ 1, 1, 1, ... 1, 1, 1 ] ] ] }' 'https://<host>/gateway/applications/mnist_app'
```
