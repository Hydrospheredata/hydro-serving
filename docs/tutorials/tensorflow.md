# Serving Tensorflow model

Tensorflow model is a model that is backed with a Tensorflow runtime. You can create a Tensorflow model just by providing a serialized into `savedmodel` format Tensorflow graph.

## Before you start

We assume you already have a @ref[deployed](../platform/) instance of the Hydrosphere platform and a @ref[CLI](../platform/cli.md) on your local machine.

To let `hs` know where the Hydrosphere platform runs, configure a new `cluster` entity.

```bash
$ hs cluster add --name local --server http://localhost
$ hs cluster use local
```

## Model serialization

### SavedModelBuilder

To export a Tensorflow model correctly, you have to identify and prepare input and output tensors. We have implemented a linear model using low-level Tensorflow API. The full code is available in our [GitHub repository](https://github.com/Hydrospheredata/hydro-serving-example/blob/master/examples/mnist_tf/train_mnist.py). In the code you can find the following lines describing input and output tensors.

```python
...
img, label = iterator.get_next()    # input image and label
...
pred = tf.nn.softmax(logits)        # output prediction
...
```

Once the model has been trained, you can export the whole computational graph and the trained weights with `tf.saved_model`. To do that you have to define the model's signature:

```python
signature_map = {
    "infer": tf.saved_model.signature_def_utils.predict_signature_def(
        inputs={"img": img}, outputs={"pred": pred})
}
```

The next step is to create a `SavedModelBuilder` object and save the model under the desired directory.

```python
builder = tf.saved_model.builder.SavedModelBuilder(export_dir)
builder.add_meta_graph_and_variables(
    sess=sess,                                          # session, where the graph was initialized
    tags=[tf.saved_model.tag_constants.SERVING],        # tag your graph as servable using this constant
    signature_def_map=signature_map)
builder.save()
```

### Estimator API

Assume that we have defined an estimator:

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

To export it we need to use the `build_raw_serving_input_receiver_fn` function and pass the result to the `export_estimator` method. `imgs` tensor, defined in the `input_receiver_fn`, should be similar to the `tf.feature_column` that is expected by the estimator.

```python
serving_input_receiver_fn = tf.estimator.export.build_raw_serving_input_receiver_fn({
    "imgs": tf.placeholder(tf.float32, shape=(None, 784))
})
estimator.export_savedmodel(export_dir, serving_input_receiver_fn)
```

## Uploading artifacts

Upload the exported model to the cluster.

```bash
$ cd {EXPORT_DIR}   # a directory with saved_model.pb file
$ hs upload --name mnist --runtime hydrosphere/serving-runtime-tensorflow-1.13.1:$project.released_version$
```

{% hint style="info" %}
You can find all available Tensorflow runtime versions @ref[here](../reference/runtimes.md).
{% endhint %}

Now the model is uploaded to the cluster, but it is not yet available for prediction.

## Creating an application

To deploy a model as a microservice, you need to create an application. You can create it manually via Hydrosphere UI, or by providing an application manifest.

```bash
$ hs apply -f - <<EOF
kind: Application
name: mnist_app
singular:
  model: mnist:1
EOF
```

## Prediction

That's it, you can now send prediction requests.

```bash
$ curl --request POST --header 'Content-Type: application/json' --header 'Accept: application/json' \ 
    --data '{ "imgs": [ [ [ 1, 1, 1, ... 1, 1, 1 ] ] ] }' 'https://<host>/gateway/applications/mnist_app'
```

