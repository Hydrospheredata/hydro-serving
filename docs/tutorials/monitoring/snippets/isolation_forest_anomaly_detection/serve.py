import collections

import numpy as np
from joblib import load

init_value = 1.0  # Default value, means that the sample is 'inlier'
window_len = 5  # Length of data sequence required for model.

window = collections.deque(maxlen=window_len)
outlier_detection_model = load('/model/files/iforest.joblib')


def infer(pickups_last_hour, pickups_next_hour):
    global window

    # serving.yaml defines that the type of input is int, so we take int_val 
    # from input sample. The pickups_next_hour parameter is a prediction of 
    # the target monitored model.

    window.append(pickups_last_hour)

    if len(window) < window_len:
        return {"value": [init_value]}
    else:
        prediction_vector = np.array(window)
        # Make a prediction
        result = outlier_detection_model.predict(prediction_vector.reshape(1, 5))
        # Pack the answer
        return {"value": result}
