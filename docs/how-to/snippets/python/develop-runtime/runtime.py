from hydro_serving_grpc.tf.api.predict_pb2 import PredictRequest, PredictResponse
from hydro_serving_grpc.tf.api.prediction_service_pb2_grpc import PredictionServiceServicer, add_PredictionServiceServicer_to_server
from hydro_serving_grpc.tf.types_pb2 import *
from hydro_serving_grpc.tf.tensor_pb2 import TensorProto
from hydro_serving_grpc.contract.model_contract_pb2 import ModelContract
from concurrent import futures

import os
import time
import grpc
import logging
import importlib


class RuntimeService(PredictionServiceServicer):
    def __init__(self, model_path, contract):
        self.contract = contract
        self.model_path = model_path
        self.logger = logging.getLogger(self.__class__.__name__)

    def Predict(self, request, context):
        self.logger.info(f"Received inference request: {request}")
        
        module = importlib.import_module("func_main")
        executable = getattr(module, self.contract.predict.signature_name)
        result = executable(**request.inputs)

        if not isinstance(result, hs.PredictResponse):
            self.logger.warning(f"Type of a result ({result}) is not `PredictResponse`")
            context.set_code(grpc.StatusCode.OUT_OF_RANGE)
            context.set_details(f"Type of a result ({result}) is not `PredictResponse`")
            return PredictResponse()
        return result


class RuntimeManager:
    def __init__(self, model_path, port):
        self.logger = logging.getLogger(self.__class__.__name__)
        self.port = port
        self.model_path = model_path
        self.server = None
        
        with open(os.path.join(model_path, 'contract.protobin')) as file:
            contract = ModelContract.ParseFromString(file.read())
        self.servicer = RuntimeService(os.path.join(self.model_path, 'files'), contract)

    def start(self):
        self.logger.info(f"Starting PythonRuntime at {self.port}")
        self.server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        add_PredictionServiceServicer_to_server(self.servicer, self.server)
        self.server.add_insecure_port(f'[::]:{self.port}')
        self.server.start()

    def stop(self, code=0):
        self.logger.info(f"Stopping PythonRuntime at {self.port}")
        self.server.stop(code)