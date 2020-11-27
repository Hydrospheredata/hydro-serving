from time import sleep

from conftest import *
from utils import get_production_subsample


@pytest.mark.parametrize("local_model, monitoring_local_model, simulated_requests",
                         [(model, model, model) for model in TEST_MODELS], indirect=True)
def test_feature_lake_requests(cluster: Cluster, application: Application, main_model_conf, local_model: LocalModel,
                               model_version: ModelVersion, simulated_requests):
    sleep(2)
    n_requests = len(simulated_requests)

    production_df = get_production_subsample(model_version.id, size=n_requests)
    assert not production_df.empty, f"No production data for model {model_version.name}:{model_version.version}," \
                                    f" in {application.name} application"
    metric_checks = production_df._hs_metric_checks.to_list()
    monitoring_models_conf = [(metric.name, metric.config.threshold_op, metric.config.threshold) for metric
                              in MetricSpec.find_by_modelversion(cluster, model_version.id)]

    for (request_id, request) in zip(production_df._hs_request_id.to_list(), metric_checks):
        for (monitoring_metric_name, comparison_operator, threshold) in monitoring_models_conf:
            assert monitoring_metric_name in request, f"{monitoring_metric_name} metric of model {model_version.name}:{model_version.version}"
