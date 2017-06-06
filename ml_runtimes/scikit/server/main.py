from flask import Flask, jsonify, request, abort
from sklearn.externals import joblib
import numpy as np
import pandas as pd
import os

app = Flask(__name__)

PORT_NUMBER = 8080
MODEL_VERSION = os.getenv('MODEL_VERSION', "version")
MODEL_NAME = os.getenv('MODEL_NAME', "name")
MODEL_TYPE = os.getenv('MODEL_TYPE', "type")


def convert_to_python(data):
    if isinstance(data, np.integer):
        return int(data)
    elif isinstance(data, np.floating):
        return float(data)
    else:
        print("{0} isn't convertible to python".format(type(data)))
        return data


def df_to_json(df):
    result = []
    cols = list(df)
    for row in df.iterrows():
        res_col = {}
        for col in cols:
            data = row[1][col]
            res_col[col] = convert_to_python(data)
        result.append(res_col)
    return result


def dict_to_df(listmap, columns):
    result = pd.DataFrame(columns=columns)
    for item in listmap:
        result = result.append(item, ignore_index=True)
    return result


@app.route('/health', methods=['GET'])
def health():
    return "Hi"

@app.route('/<model_name>', methods=['GET'])
def prepare(model_name):
    pass

@app.route('/<model_name>', methods=['POST'])
def predict(model_name):
    try:
        model = joblib.load('../models/{0}/model.pkl'.format(model_name))
        input_columns = joblib.load('../models/{0}/input_columns.pkl'.format(model_name))
        output_columns = joblib.load('../models/{0}/output_columns.pkl'.format(model_name))

        input_data = request.json
        df = dict_to_df(input_data, input_columns)

        prediction = model.predict(df)
        res_df = pd.DataFrame(data=prediction, columns=output_columns)

        print(str(df) + '\nPrediction:\n' + str(res_df))
        return jsonify(df_to_json(res_df))
    except FileNotFoundError:
        print('Unknown model: ' + model_name)
        abort(404)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080)
