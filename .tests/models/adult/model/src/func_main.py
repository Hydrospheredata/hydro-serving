import hydro_serving_grpc as hs
import numpy as np
from joblib import load
import pandas as pd
clf = load('/model/files/classification_model.joblib')

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
    # extracted = np.array([extract_value(kwargs[feature]) for feature in features])
    X = pd.DataFrame.from_dict({'input': kwargs}, orient='index', columns=features)
    # transformed = np.dstack(extracted).reshape(1, len(features))
    predicted = clf.predict(X)
    #
    # response = hs.TensorProto(
    #     int64_val=[predicted.item()],
    #     dtype=hs.DT_INT64,
    #     tensor_shape=hs.TensorShapeProto())

    return {"classes": predicted.item()}
