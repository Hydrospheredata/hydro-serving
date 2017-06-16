import os
import urllib.request
import json
import time
import sys
import logging
import numpy as np
import pandas as pd
from flask import Flask, jsonify, request, abort
from sklearn.externals import joblib

from ebay_product_db import EbayJsonFilesBasedProductDB
from cfepm.facade import ProductMatchingFacade, ITEM_SPECS_KEY, ITEM_TITLE_KEY
from cfepm.data.model import Item
from cfepm.fe.feature_extractors import parse_specs
from utils import *
from ml_repository import *

ADDR = os.getenv("SERVE_ADDR", "0.0.0.0")
PORT = int(os.getenv("SERVE_PORT", "9090"))

MODEL_NAME = os.getenv('MODEL_NAME', "pm_scikit")

ML_REPO_ADDR = os.getenv('ML_REPO_ADDR', 'localhost')
ML_REPO_PORT = os.getenv('ML_REPO_PORT', '8081')

ITEM_TITLE_KEY = 'Title'
ITEM_SPECS_KEY = 'ItemSpecifics'
logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
log = logging.getLogger()
app = Flask(__name__)
log.info("Server @ {0}:{1}".format(ADDR, PORT))

repo = MLRepository(ML_REPO_ADDR, ML_REPO_PORT)
log.info("Repository @ {0}:{1}".format(ML_REPO_ADDR, ML_REPO_PORT))

metadata = repo.get_metadata(MODEL_NAME)
files = repo.get_files(MODEL_NAME)
log.info(metadata)
log.info(files)

for file in files:
    repo.download_file(MODEL_NAME, file)
pipeline = repo.load_model(MODEL_NAME)
log.info("Model is loaded and ready to serve.")


@app.route('/health', methods=['GET'])
def health():
    log.info("Healthcheck")
    return "Hi"


@app.route('/<model_name>', methods=['POST'])
def predict(model_name):
    """

    :param model_name: Ignored
    :return:
    """
    input_columns = metadata['inputs']
    output_columns = metadata['outputs']
    input_data = request.json
    in_df = dict_to_df(input_data, input_columns)

    pairs = []
    for row in input_data:
        specs_1 = parse_specs(row['ItemSpecifics1'])
        item_1 = Item(row['Title1'], specs_1, None)
        specs_2 = parse_specs(row['ItemSpecifics2'])
        item_2 = Item(row['Title2'], specs_2, None)
        pairs.append((item_1, item_2))
    results = pipeline.transform(pairs).tolist()
    res_df = pd.DataFrame()
    res_df[output_columns[0]] = list(results)

    joined = in_df.join(res_df)
    log.info(str(joined))
    return jsonify(df_to_json(joined))


if __name__ == '__main__':
    app.run(host=ADDR, port=PORT)
