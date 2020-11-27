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
    extracted = np.array([kwargs[feature] for feature in features])
    transformed = np.dstack(extracted).reshape(1, len(features))
    predicted = monitoring_model.decision_function(transformed)
    return {"value": predicted.astype("double").item()}
