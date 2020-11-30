from conftest import *
from hydroserving.core.model.parser import Model as CLIModel


@pytest.mark.parametrize("local_model", TEST_MODELS, indirect=True)
def test_model_upload(cluster: Cluster, local_model: LocalModel):
    model_version = local_model.upload(cluster)
    model_version.lock_till_released(TIMEOUT_SEC)


@pytest.mark.parametrize("local_model, monitoring_local_model, main_model_conf",
                         [(model, model, model) for model in TEST_MODELS], indirect=True)
def test_creating_monitoring_metric(cluster: Cluster, model_version: ModelVersion, monitoring_modelversion,
                                    main_model_conf: CLIModel):
    model = main_model_conf.name
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

    cluster_monitoring_metrics = MetricSpec.find_by_modelversion(cluster, model_version.id)

    assert len(cluster_monitoring_metrics) == 1, f"Model {model} is expected to have one cluster metric"

    cluster_metric = cluster_monitoring_metrics[0]
    metric_name = cluster_metric.name
    metric_model = cluster_metric.config.servable['modelVersion']['model']['name']
    metric_model_version = cluster_metric.config.servable['modelVersion']['modelVersion']
    assert int(metric_model_version) == int(
        monitoring_modelversion.version), f"Model {metric_model} in {metric_name} metric is expected to have model version {monitoring_modelversion.id}. " \
                                          f"But has {metric_model_version}"
    assert monitoring_modelversion.name == metric_model, f"Metric {metric_name} is expected to have model {monitoring_modelversion.name}. " \
                                                         f"But has {metric_model}"
