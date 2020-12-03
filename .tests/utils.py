import json
import os
from enum import Enum

import pandas as pd
import requests
from config import *
from hydrosdk import LocalModel, DockerImage
from hydroserving.core.model.parser import parse_model
from hydroserving.util.yamlutil import yaml_file

CLUSTER_URL = os.getenv("CLUSTER_URL", 'http://localhost:80')


def get_production_subsample(model_id, size=1000) -> pd.DataFrame:
    url = f'{CLUSTER_URL}/monitoring/checks/subsample/{model_id}?size={size}'
    r = requests.get(url)
    if r.status_code == 200:
        return pd.DataFrame.from_dict(r.json())
    if r.status_code == 404:
        return pd.DataFrame()
    else:
        raise Exception(f'Couldn\'t get production data (url): {r.status_code}')


def read_model_conf(model_root):
    yaml_path = os.path.join(model_root, SERVING_YAML_FILE)
    with open(yaml_path) as file:
        model = parse_model(yaml_file(file))
    return model


def get_training_data_path(modelversion_id):
    response = requests.get(f'{CLUSTER_URL}/monitoring/training_data?modelVersionId={modelversion_id}')
    training_data_s3 = json.loads(response.text)
    return training_data_s3
