from typing import Dict, Tuple

import pytest
import s3fs
from grpc import ssl_channel_credentials
from hydrosdk import LocalModel, DockerImage, ModelVersion, MetricSpecConfig, MetricSpec
from hydrosdk.application import ExecutionStageBuilder, ApplicationBuilder, Application
from hydrosdk.cluster import Cluster
from hydrosdk.modelversion import MonitoringConfiguration
from hydrosdk.servable import Servable
from hydroserving.core.application.parser import parse_application
from hydroserving.core.model.parser import Model as CLIModel
from hydroserving.util.yamlutil import yaml_file

from config import *
from simulate_traffic_all import simulate_traffic
from utils import read_model_conf, ThresholdCmpOp


def init_s3():
    if AWS_STORAGE_ENDPOINT:  # if minio
        s3 = s3fs.S3FileSystem(
            anon=False,
            client_kwargs={
                'endpoint_url': AWS_STORAGE_ENDPOINT
            },
            config_kwargs={'s3': {'addressing_style': 'path'}}
        )
    else:
        s3 = s3fs.S3FileSystem()
    return s3


_test_failed_incremental: Dict[str, Dict[Tuple[int, ...], str]] = {}
s3 = init_s3()


@pytest.fixture(scope="session")
def cluster():
    if GRPC_CLUSTER_ENDPOINT_SSL:
        return Cluster(HTTP_PROXY_ADDRESS, GRPC_PROXY_ADDRESS, ssl=True,
                       grpc_credentials=ssl_channel_credentials())
    else:
        return Cluster(HTTP_PROXY_ADDRESS, GRPC_PROXY_ADDRESS)


@pytest.fixture(scope="session", params=TEST_MODELS)
def main_model_conf(request):
    model = request.param
    model_root = os.path.join(MODELS_PATH, model, 'model')
    return read_model_conf(model_root)


@pytest.fixture(scope="session", params=TEST_MODELS)
def monitoring_model_conf(request):
    model = request.param
    model_root = os.path.join(MODELS_PATH, model, 'monitoring_model')
    return read_model_conf(model_root)


@pytest.fixture(scope="session", params=TEST_MODELS)
def application_conf(request):
    model = request.param
    yaml_path = os.path.join(MODELS_PATH, model, SERVING_YAML_FILE)
    with open(yaml_path) as file:
        app_conf = parse_application(yaml_file(file))
    return app_conf


@pytest.fixture(scope="session")
def monitoring_local_model(request):
    model = request.param
    model_root = os.path.join(MODELS_PATH, model, 'monitoring_model')
    model = read_model_conf(model_root)
    monitoring_configuration = MonitoringConfiguration(batch_size=10)
    model = LocalModel(name=model.name, runtime=DockerImage(model.runtime.name, model.runtime.tag, None),
                       path=model_root, payload=model.payload,
                       contract=model.contract, metadata={'created_by': 'test local monitoring model'},
                       install_command=model.install_command,
                       training_data=model.training_data_file,
                       monitoring_configuration=monitoring_configuration)
    return model


@pytest.fixture(scope="session")
def local_model(request):
    model = request.param
    model_root = os.path.join(MODELS_PATH, model, 'model')
    model_conf = read_model_conf(model_root)
    lm = LocalModel(name=model_conf.name, runtime=DockerImage(model_conf.runtime.name, model_conf.runtime.tag, None),
                    path=model_root, payload=model_conf.payload,
                    contract=model_conf.contract, metadata={'created_by': 'test local model'},
                    install_command=model_conf.install_command,
                    training_data=model_conf.training_data_file)
    return lm


@pytest.fixture(scope="session")
def monitoring_modelversion(cluster: Cluster, monitoring_local_model: LocalModel):
    mv: ModelVersion = monitoring_local_model.upload(cluster)
    mv.lock_till_released(TIMEOUT_SEC)
    return mv


@pytest.fixture(scope="session")
def model_version(cluster: Cluster, local_model: LocalModel):
    mv: ModelVersion = local_model.upload(cluster)
    mv.lock_till_released(TIMEOUT_SEC)
    return mv


@pytest.fixture(scope="session")
def model_version_with_monitoring(cluster: Cluster, model_version: ModelVersion, main_model_conf: CLIModel,
                                  monitoring_modelversion: ModelVersion):
    mv = model_version
    monitoring_servable = Servable.create(cluster, monitoring_modelversion.name, monitoring_modelversion.version,
                                          metadata={'created_by': f'e2e_test',
                                                    'monitored_model': f'{model_version.name}v{model_version.version}'},
                                          deployment_configuration=None)
    monitoring_servable.lock_while_starting(TIMEOUT_SEC)

    monitoring_config_dict: Dict = main_model_conf.monitoring[0]
    metric_config: MetricSpecConfig = MetricSpecConfig(modelversion_id=monitoring_modelversion.id,
                                                       threshold=monitoring_config_dict['config']['threshold'],
                                                       threshold_op="LessEq")
    metric_spec = MetricSpec.create(cluster, name=monitoring_config_dict['name'],
                                    modelversion_id=model_version.id, config=metric_config)
    return mv


@pytest.fixture(scope="session")
def application(cluster: Cluster, model_version_with_monitoring: ModelVersion):
    assert type(model_version_with_monitoring) == ModelVersion
    stage = ExecutionStageBuilder() \
        .with_model_variant(model_version_with_monitoring, 100) \
        .build()
    app = ApplicationBuilder(cluster, f"{model_version_with_monitoring.name}v{model_version_with_monitoring.id}") \
        .with_stage(stage) \
        .build()
    app.lock_while_starting(TIMEOUT_SEC)
    return app


@pytest.fixture(scope="session", params=TEST_MODELS)
def simulated_requests(cluster: Cluster, application: Application, request):
    model = request.param
    predictor = application.predictor()
    sent_requests = simulate_traffic(model, predictor, demo_length=DATA_SAMPLE_SIZE)
    return sent_requests
