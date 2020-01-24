import numpy as np
import hydro_serving_grpc as hs
from joblib import dump, load
import collections

init_value = 1.0  # Default value, means that the sample is 'inlier'
window_len = 5  # Length of data sequence required for model.

window = collections.deque(maxlen=window_len)
outlier_detection_model = load('/model/files/iforest.joblib')


def infer(pickups_last_hour, pickups_next_hour):
    global window

    # serving.yaml defines that the type of input is int, so we take int_val 
    # from input sample. The pickups_next_hour parameter is a prediction of 
    # the target monitored model.
    input_value = int(pickups_last_hour.int_val[0])

    if len(window) < window_len-1:
        window.append(input_value)
        return pack_predict(init_value)
    else:
        window.append(input_value)
        prediction_vector = np.array(window)
        # Make a prediction
        result = outlier_detection_model.predict(prediction_vector.reshape(1, 5))
        # Pack the answer
        return pack_predict(result[0])


def pack_predict(result):
    tensor = hs.TensorProto(
        dtype=hs.DT_DOUBLE,
        double_val=[result],
        tensor_shape=hs.TensorShapeProto()
    )
    return hs.PredictResponse(outputs={"value": tensor})