import os
from typing import List

import numpy as np
import pandas as pd
from config import MODELS_PATH
from hydrosdk.application import PredictServiceClient
from pandas import DataFrame
from tqdm.auto import tqdm


def simulate_adult(predictor: PredictServiceClient, demo_length=20) -> List[np.array]:
    model_path = os.path.join(MODELS_PATH, 'adult')
    data: DataFrame = pd.read_csv(os.path.join(model_path, "data", "profile.csv"), header=0)
    data = data.drop(columns=['prediction'])
    sent_requests = []
    data = data.sample(demo_length, replace=False)
    for x in tqdm(data.to_dict('records')[:demo_length]):
        result = predictor.predict(x)
        sent_requests.append(np.array(x.values()))
    return sent_requests


def simulate_nyc(predictor: PredictServiceClient, demo_length=20):
    model_path = os.path.join(MODELS_PATH, 'nyc_taxi')

    data = pd.read_csv(os.path.join(model_path, "data", "taxi_pickups.csv"))
    sent_requests = []
    for x in tqdm(data.to_dict('records')[:demo_length]):
        print(x)
        result = predictor.predict({'pickups_last_hour': x['pickups']})
        sent_requests.append(np.array(x))
    return sent_requests


def simulate_traffic(model_name, predictor: PredictServiceClient, demo_length=20):
    if model_name == 'adult':
        sent_requests = simulate_adult(predictor, demo_length=demo_length)
        return sent_requests
    if model_name == 'nyc_taxi':
        sent_requests = simulate_nyc(predictor, demo_length=demo_length)
        return sent_requests
