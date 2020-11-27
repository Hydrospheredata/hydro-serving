import os

TEST_MODELS = ['adult']
MODELS_PATH = os.getenv('MODELS_PATH', 'models')
SERVING_YAML_FILE = 'serving_test.yaml'
TIMEOUT_SEC = 2400
BATCH_SIZE = 10
DATA_SAMPLE_SIZE = BATCH_SIZE + 10

CLUSTER_NAME = os.getenv("CLUSTER_NAME", 'local')
HTTP_PROXY_ADDRESS = os.getenv("HTTP_PROXY_ADDRESS", 'http://localhost:80')
GRPC_PROXY_ADDRESS = os.getenv("GRPC_PROXY_ADDRESS", 'localhost:9090')
GRPC_CLUSTER_ENDPOINT_SSL = bool(os.getenv('SECURE', False))
AWS_STORAGE_ENDPOINT = os.getenv('AWS_STORAGE_ENDPOINT', 'http://localhost:9000')
