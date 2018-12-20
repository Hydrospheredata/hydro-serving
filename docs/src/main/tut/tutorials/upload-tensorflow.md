---
layout: docs
title:  "Upload Tensorflow Model"
permalink: "tensorflow.html"
---

# Upload Tensorflow Model

Deploying Tensorflow models does not require any additional contract writings since ML Lambda can automatically infer models' signatures. Running Tensorflow models can be done with `hydrosphere/serving-runtime-tensorflow` runtime. The only thing you have to worry about is how to properly export your model with `tf.saved_model`.

In this page we will cover how to create a simple Tensorflow classifier using different APIs, train it and deploy on ML Lambda.

- [Basic API]({{site.baseurl}}{%link tutorials/upload-tensorflow.md%}#basic-api)
- [Estimator API]({{site.baseurl}}{%link tutorials/upload-tensorflow.md%}#estimator-api)

<br>
<br>

## Basic API

In this example we will create a linear MNIST classifier. The full code is available in the GitHub repository. 

### Prepare data 

```python
mnist = input_data.read_data_sets("data/mnist")

train_img, train_lbl = mnist.train.images, mnist.train.labels
test_img, test_lbl = mnist.test.images, mnist.test.labels

train = (train_img, tf.one_hot(train_lbl, 10))
test = (test_img, tf.one_hot(test_lbl, 10))

train_data = tf.data.Dataset.from_tensor_slices(train).shuffle(training_size).batch(batch_size)
test_data = tf.data.Dataset.from_tensor_slices(test).shuffle(test_size).batch(batch_size)

iterator = tf.data.Iterator.from_structure(
    train_data.output_types, train_data.output_shapes)
img, label = iterator.get_next()

train_init = iterator.make_initializer(train_data)	# initializer for train_data
test_init = iterator.make_initializer(test_data)	# initializer for test_data
```

This will create a `data/mnist` directory and download there all MNIST related data. From this we will create a `tf.data.Iterator` and use it in the training/testing stages.

### Create a classifier 

Next, we'll define the model. 

```python
# Define model 
weights = tf.get_variable("weight", shape=[784, 10], initializer=tf.truncated_normal_initializer(stddev=0.01))
bias = tf.get_variable("bias", shape=[10], initializer=tf.zeros_initializer)

logits = tf.matmul(img, weights) + bias
pred = tf.nn.softmax(logits)
```

Here we first pass the image through the linear function. The logits obtained from that function will be used to classify images after normalization with softmax function. Now we need to define a loss function and an optimizer for that. 

```python
# Define loss
entropy = tf.nn.softmax_cross_entropy_with_logits_v2(
    logits=logits, labels=tf.stop_gradient(label))
loss = tf.reduce_mean(entropy)

# Define optimizer
optimizer = tf.train.AdamOptimizer(learning_rate=learning_rate).minimize(loss)
```

### Train the model

```python
# Run training 
for i in range(n_epochs): 	
    sess.run(train_init)
    total_loss, n_batches = 0, 0
    try:
        while True:
            _, l = sess.run([optimizer, loss])
            total_loss += l
            n_batches += 1
    except tf.errors.OutOfRangeError:
        pass
    print('Average loss epoch {0}: {1}'.format(i, total_loss/n_batches))

# Run evaluation
sess.run(test_init)
total_correct_preds = 0
try:
    while True:
        accuracy_batch = sess.run(accuracy)
        total_correct_preds += accuracy_batch
except tf.errors.OutOfRangeError:
    pass

print('Accuracy {0}'.format(total_correct_preds/test_size))
```
Training the linear model does not take too long. After the training the accuracy should hold around 91%. 

### Save the model

```python
# Save model
signature_map = {
    "infer": tf.saved_model.signature_def_utils.predict_signature_def(
        inputs={"img": img}, outputs={"pred": pred})
}

builder = tf.saved_model.builder.SavedModelBuilder(export_dir)
builder.add_meta_graph_and_variables(
    sess=sess, 
    tags=[tf.saved_model.tag_constants.SERVING],
    signature_def_map=signature_map)
builder.save()
```

We use `tf.saved_model` in order to save the graph and its variables. We need to define a `signature_map` which itself defines the inputs and the ouputs of the model. During the model exporting the new directory `export_dir` will be created and the model will be saved there. 

After that the saved model can be deployed.

```sh 
$ cd ${EXPORT_DIR}
$ hs upload
```

<br>
<br>

## Estimator API

In this example we will export MNIST classifier based on premade `tf.estimator.DNNClassifier` class. 

### Prepare data

```python
def input_fn(path="data/mnist", data="train", batch_size=256):
    mnist = input_data.read_data_sets("data/mnist")
    data_imgs = getattr(mnist, data).images
    data_labels = getattr(mnist, data).labels.astype(np.int32)
    data_tuple = (data_imgs, data_labels)

    dataset = (tf.data.Dataset
        .from_tensor_slices(data_tuple)
        .batch(batch_size))
    iterator = dataset.make_one_shot_iterator()
    imgs, labels = iterator.get_next()
    return {"imgs": imgs}, labels
```

### Create and train a classifier

```python
imgs = tf.feature_column.numeric_column("imgs", shape=(784,))
estimator = tf.estimator.DNNClassifier(
    hidden_units=[256, 64],
    feature_columns=[imgs],
    n_classes=10,
    model_dir='models/mnist')

train_spec = tf.estimator.TrainSpec(
    input_fn=lambda: input_fn(data="train"), max_steps=10000)
eval_spec = tf.estimator.EvalSpec(
    input_fn=lambda: input_fn(data="test"))

tf.estimator.train_and_evaluate(estimator, train_spec, eval_spec)
```

### Save the model 

Saving estimator model is a bit easier than the model in Basic API example. Here we only need to provide a mapping which is similiar to the `feature_columns` used by estimator. 

```python
serving_input_receiver_fn = tf.estimator.export.build_raw_serving_input_receiver_fn({
    "imgs": tf.placeholder(tf.float32, shape=(None, 784))})
estimator.export_savedmodel("servables/mnist_dnn", serving_input_receiver_fn)
```

Again, after the model has been trained and saved, we can upload it to ML Lambda.

```sh 
$ cd ${EXPORT_DIR}
$ hs upload
```

# What's next?

- Learn, how to work with CLI;
- Learn, how to invoke models;