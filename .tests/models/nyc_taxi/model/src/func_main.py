import numpy as np
from joblib import load

global window
import collections

# Length of data sequence required for model.
window_len = 5
# Use average value (among the training dataset) as a prediction result until the minimum number of samples (window_len) is collected.
average_value = 334

window = collections.deque(maxlen=window_len)
monitoring_model = load('/model/files/model.joblib')


def infer(pickups_last_hour):
    global window
    # Take int_val from input sample.
    input_value = int(pickups_last_hour)

    if len(window) < window_len - 1:
        window.append(input_value)
        return pack_predict(average_value)
    else:
        window.append(input_value)
        prediction_vector = np.array(window)
        # 2. Make a prediction
        result = monitoring_model.predict(prediction_vector.reshape(1, 5))
        # 3. Pack the answer
        return pack_predict(result[0])


def pack_predict(result):
    ret = int(round(result))
    return {"pickups_next_hour": ret}
