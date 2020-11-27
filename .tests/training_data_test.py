import os

import pandas as pd
import pytest
from conftest import s3
from hydrosdk import ModelVersion, LocalModel
from utils import get_training_data_path, TEST_MODELS


@pytest.mark.parametrize("local_model", TEST_MODELS, indirect=True)
def test_training_data(local_model: LocalModel, model_version: ModelVersion):
    if local_model.training_data:
        model_version.training_data = local_model.training_data
        model_version.upload_training_data()

    training_data_path = local_model.training_data
    training_data_cluster_path = get_training_data_path(model_version.id)

    if training_data_path and not training_data_cluster_path:
        raise Exception(f'Training data {training_data_path} is absent in S3')
    if not training_data_path and training_data_cluster_path:
        raise Exception(
            f'Training data on S3 {training_data_cluster_path} should not be present, as not local training data is found')
    if training_data_cluster_path:
        s3_link = training_data_cluster_path[0]
        assert s3.exists(s3_link), f"No such file on s3: {s3_link}"
        training_s3_csv = pd.read_csv(s3.open(s3_link, mode='rb'))
        assert training_s3_csv is not None, f"Couldn\'t read {s3_link}"
        training_csv = pd.read_csv(os.path.join(training_data_path))
        assert training_csv is not None, f"Couldn\'t read {training_data_path}"
        assert training_csv.equals(
            training_s3_csv), f"S3 data ({s3_link}) is not the same as training data ({training_data_path})"
