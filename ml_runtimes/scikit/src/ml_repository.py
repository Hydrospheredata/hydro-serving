import urllib.request
import json
import os
from sklearn.externals import joblib


class MLRepository:
    def __init__(self, addr, port):
        self.host = "http://{0}:{1}".format(addr, port)

    def get_metadata(self, model_name):
        return json.load(urllib.request.urlopen("{0}/metadata/{1}".format(self.host, model_name)))

    def get_files(self, model_name):
        return json.load(urllib.request.urlopen("{0}/files/{1}".format(self.host, model_name)))

    def download_file(self, model_name, file_name):
        url = "{0}/download/{1}/{2}".format(self.host, model_name, file_name)
        save_path = "models/{0}/{1}".format(model_name, file_name)

        if not os.path.exists(os.path.dirname(save_path)):
            os.makedirs(os.path.dirname(save_path))

        urllib.request.urlretrieve(url, save_path)

        return save_path

    def load_model(self, model_name):
        model_path = "models/{0}/model.pkl".format(model_name)
        return joblib.load(model_path)