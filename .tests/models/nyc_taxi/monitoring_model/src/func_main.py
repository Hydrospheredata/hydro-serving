import numpy as np
import hydro_serving_grpc as hs
from joblib import dump, load
import collections


global window
# Default value, means sample is 'inlier'
init_value = 1.0
# Length of data sequence required for model.
window_len = 5

window = collections.deque(maxlen=window_len)
outlier_detection_model = load('/model/files/iforest.joblib')



def infer(pickups_last_hour, pickups_next_hour):
    global window

    # Contract (serving.yaml) defines that type of input is int,
    # so take int_val from input sample.
    input_value = int(pickups_last_hour)
    # The pickups_next_hour parameter is a prediction of monitoring_model.
    # It is optional for use depending on particular outlier detection technique.
    # (not used in this method)

    if len(window) < window_len-1:
        window.append(input_value)
        return pack_predict(init_value)
    else:
        window.append(input_value)
        prediction_vector = np.array(window)
        # 2. Make a prediction
        result = outlier_detection_model.predict(prediction_vector.reshape(1, 5))
        # 3. Pack the answer
        return pack_predict(result[0]*1.0)

def pack_predict(result):
    return {"value": result}
