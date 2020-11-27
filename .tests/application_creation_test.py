from conftest import *


@pytest.mark.parametrize("local_model", TEST_MODELS, indirect=True)
def test_application_creation(cluster: Cluster, model_version: ModelVersion, local_model: LocalModel):
    stage = ExecutionStageBuilder() \
        .with_model_variant(model_version, 100) \
        .build()
    app = ApplicationBuilder(cluster, f"{model_version.name}v{model_version.id}") \
        .with_stage(stage) \
        .build()
    app.lock_while_starting(600)
