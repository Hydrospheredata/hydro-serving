import hydro_serving_grpc as hs
import numpy as np
from joblib import load

monitoring_model = load('/model/files/monitoring_model.joblib')

features = ['age',
            'workclass',
            'education',
            'marital_status',
            'occupation',
            'relationship',
            'race',
            'sex',
            'capital_gain',
            'capital_loss',
            'hours_per_week',
            'country']


def extract_value(proto):
    return np.array(proto.int64_val, dtype='int64')[0]


def predict(**kwargs):
    extracted = np.array([extract_value(kwargs[feature]) for feature in features])
    transformed = np.dstack(extracted).reshape(1, len(features))
    predicted = monitoring_model.decision_function(transformed)

    response = hs.TensorProto(
        double_val=[predicted.item()],
        dtype=hs.DT_DOUBLE,
        tensor_shape=hs.TensorShapeProto())

    return hs.PredictResponse(outputs={"value": response})
