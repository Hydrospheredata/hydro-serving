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


class ThresholdCmpOp(Enum):
    """
    Threshold comparison operator is used to check if ModelVersion is healthy
    Model is healthy if {metric_value}{TresholdCmpOp}{threshold}
    """
    EQ = "Eq"
    NOT_EQ = "NotEq"
    GREATER = "Greater"
    GREATER_EQ = "GreaterEq"
    LESS = "Less"
    LESS_EQ = "LessEq"


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


def local_monitoring_model(model):
    model_root = os.path.join(MODELS_PATH, model, 'monitoring_model')
    model_conf = read_model_conf(model_root)
    lm: LocalModel = LocalModel(name=model_conf.name,
                                runtime=DockerImage(model_conf.runtime.name, model_conf.runtime.tag, None),
                                path=model_root, payload=model_conf.payload,
                                contract=model_conf.contract, metadata=model_conf.metadata,
                                install_command=model_conf.install_command,
                                training_data=model_conf.training_data_file)
    return lm
