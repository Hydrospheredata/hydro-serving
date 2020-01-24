import numpy as np
import tensorflow as tf
import hydro_serving_grpc as hs
from keras.models import load_model

# 0. Load model once
model = load_model('/model/files/model.h5')
graph = tf.get_default_graph() 

def infer(x):
    # 1. Retrieve tensor's content and put it to numpy array
    data = np.array(x.double_val)
    data = data.reshape([dim.size for dim in x.tensor_shape.dim])

    # 2. Make a prediction
    with graph.as_default():
        result = model.predict(data)
    
    # 3. Pack the answer
    y_shape = hs.TensorShapeProto(dim=[hs.TensorShapeProto.Dim(size=-1)])
    y_tensor = hs.TensorProto(
        dtype=hs.DT_DOUBLE,
        double_val=result.flatten(),
        tensor_shape=y_shape)

    # 4. Return the result
    return hs.PredictResponse(outputs={"y": y_tensor})