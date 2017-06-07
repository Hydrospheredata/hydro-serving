import os
import urllib.request
import json
import numpy as np
import pandas as pd
from flask import Flask, jsonify, request, abort
from sklearn.externals import joblib

from server.src.scikit_metadata import *
from server.src.utils import *
from server.src.ml_repository import *

ADDR = os.getenv("SERVE_ADDR", "0.0.0.0")
PORT = int(os.getenv("SERVE_PORT", "8080"))

MODEL_VERSION = os.getenv('MODEL_VERSION', "version")
MODEL_NAME = os.getenv('MODEL_NAME', "name")
MODEL_TYPE = os.getenv('MODEL_TYPE', "type")

ML_REPO_ADDR = os.getenv('ML_REPO_ADDR', '192.168.99.100')
ML_REPO_PORT = os.getenv('ML_REPO_PORT', '8081')

app = Flask(__name__)
repo = MLRepository(ML_REPO_ADDR, ML_REPO_PORT)

model_cache = {}

@app.route('/health', methods=['GET'])
def health():
    return "Hi"


@app.route('/<model_name>', methods=['POST'])
def predict(model_name):
    if model_name in model_cache:
        imported_model = model_cache[model_name].model
        input_columns = model_cache[model_name].inputs
        output_columns = model_cache[model_name].outputs
    else:
        metadata = repo.get_metadata(model_name)
        files = repo.get_files(model_name)
        print(metadata)
        print(files)
        for file in files:
            repo.download_file(model_name, file)
        imported_model = repo.load_model(model_name)
        input_columns = metadata['inputs']
        output_columns = metadata['outputs']
        model_cache[model_name] = ScikitMetadata(imported_model, input_columns, output_columns)

    input_data = request.json
    input_data_keys = list(input_data[0].keys())
    if set(output_columns).issubset(set(input_data_keys)):
        print("ERROR Same columns in input and output")
        abort(400, "Same columns in input and output.\nInput:{0}\nOutput:{1}".format(input_data_keys, output_columns))
    df = dict_to_df(input_data, input_columns)
    print(df)
    prediction = imported_model.predict(df[input_columns])
    res_df = pd.DataFrame(data=prediction, columns=output_columns)

    print(str(df) + '\nPrediction:\n' + str(res_df))
    return jsonify(df_to_json(df.join(res_df)))


if __name__ == '__main__':
    app.run(host=ADDR, port=PORT)
