import tensorflow as tf
from keras.models import load_model

# Load model once
model = load_model('/model/files/model.h5')
graph = tf.get_default_graph()


def infer(x):
    # Make a prediction
    with graph.as_default():
        y = model.predict(x)

    # Return the result
    return {"y": y.flatten()}
