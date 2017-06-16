import falcon
from falcon_cors import CORS
import logging
import json
import pandas as pd
from argparse import ArgumentParser
from wsgiref import simple_server
from cfepm.facade import ProductMatchingFacade, ITEM_SPECS_KEY, ITEM_TITLE_KEY
from cfepm.product_db.ebay_product_db import EbayJsonFilesBasedProductDB
import codecs
from cfepm.data.model import \
    DF_COL_ITEM_SPECS, DF_COL_PROD_SPECS, DF_COL_PROD_DESC, DF_COL_ITEM_TITLE, DF_COL_PROD_TITLE
from sklearn.externals import joblib
import os

JSON_ROW_ALLOWED_COLUMNS = frozenset({
    DF_COL_ITEM_TITLE, DF_COL_PROD_TITLE, DF_COL_PROD_SPECS, DF_COL_ITEM_SPECS, DF_COL_PROD_DESC
})


class ClassifierAnswerResource:
    def __init__(self, facade):
        self.facade = facade

    def on_post(self, req, resp):
        log = logging.getLogger(__name__)
        log.debug("Request: %s", req)
        # parse input
        if req.content_length:
            try:
                req_body = req.stream.read()
                if isinstance(req_body, bytes):
                    req_body = codecs.decode(req_body, "UTF-8")
                input_dicts = json.loads(req_body)
            except Exception as ex:
                return _bad_request(resp, "Can't parse request body as JSON, error: %s", str(ex))
            if not isinstance(input_dicts, list):
                return _bad_request(resp, "A root JSON array is expected")
            if not input_dicts:
                # return empty array result
                resp.body = json.dumps([])
                return
            # validate column names
            for row_dict in input_dicts:
                for col, val in row_dict.items():
                    if col not in JSON_ROW_ALLOWED_COLUMNS:
                        return _bad_request(resp, "Unknown field (column name) : %s", col)
                    if val is not None and not isinstance(val, str):
                        return _bad_request(resp,
                                            "There are non-string and non-null field (column '%s') value : %s",
                                            col, val)
            input_df = pd.DataFrame(input_dicts)
        else:
            return _bad_request(resp, "Empty request body")
        # predict
        yes_prob_arr = self.facade.classify_pairs_df(input_df)
        # send output
        result_list = [p for p in yes_prob_arr]
        resp.body = json.dumps(result_list)


class ProductMatchingResource:
    def __init__(self, facade_dict):
        self.facade_dict = facade_dict

    def on_post(self, req, resp, category):
        log = logging.getLogger(__name__)
        # match category to its facade
        facade = self.facade_dict.get(category)
        if facade is None:
            resp.body = "Unknown category: %s" % category
            resp.status = falcon.HTTP_404
            return
        # parse input
        if req.content_length:
            try:
                req_body = req.stream.read()
                if isinstance(req_body, bytes):
                    req_body = codecs.decode(req_body, "UTF-8")
                log.debug("Request body:\n%s", req_body)
                input_dict = json.loads(req_body)
            except Exception as ex:
                return _bad_request(resp, "Can't parse request body as JSON, error: %s", str(ex))
            if not isinstance(input_dict, dict):
                return _bad_request(resp, "A root JSON object is expected")
            # validate fields
            if ITEM_TITLE_KEY not in input_dict:
                return _bad_request(resp, "Field '%s' is mandatory", ITEM_TITLE_KEY)
            item_title = input_dict[ITEM_TITLE_KEY]
            if not isinstance(item_title, str):
                return _bad_request(resp, "%s must be a string", ITEM_TITLE_KEY)
            if ITEM_SPECS_KEY in input_dict:
                spec_dict = input_dict[ITEM_SPECS_KEY]
                if not isinstance(spec_dict, dict):
                    return _bad_request(resp, "Object value of '%s' is expected", ITEM_SPECS_KEY)
                for k, v in spec_dict.items():
                    if not isinstance(k, str) or not isinstance(v, str):
                        return _bad_request(resp, "Only string key-values are allowed in %s", ITEM_SPECS_KEY)
                        #
        else:
            return _bad_request(resp, "Empty request body")
        #
        kwargs = {}
        top_n = req.params.get('n', None)
        if top_n is not None:
            try:
                top_n = int(top_n)
                kwargs['top_n'] = top_n
            except:
                return _bad_request(resp, "query param 'n' must be an integer")
        #
        min_prob = req.params.get('minprob', None)
        if min_prob is not None:
            try:
                min_prob = float(min_prob)
                if min_prob < 0:
                    min_prob = 0
                elif min_prob > 1:
                    min_prob = 1
                kwargs['min_prob'] = min_prob
            except:
                return _bad_request(resp, "query param 'minprob' must be a float")
        # invoke back-end
        scored_product_list = facade.get_top_matches(input_dict, **kwargs)
        # send output
        resp.body = json.dumps(scored_product_list, indent=2)


class CategoryResource:
    def __init__(self, category_list):
        self.category_list = category_list
        self.category_dict = {
            cat['id']: cat
            for cat in category_list
        }
        # validate
        for cat in self.category_list:
            if not cat.get('label'):
                raise ValueError("No label for category with id '%s'" % cat['id'])

    def on_get(self, req, resp):
        log = logging.getLogger(__name__)
        log.debug("Request: %s", req)
        #
        resp.body = json.dumps(self.category_list, indent=2)

    @staticmethod
    def read_from_json_filepath(fp):
        log = logging.getLogger(__name__)
        with open(fp) as inp:
            category_list = json.load(inp)
        if not isinstance(category_list, list):
            raise ValueError("Root array was expected in %s" % fp)
        log.info("Category list: %s", '\n'.join(str(cat) for cat in category_list))
        return CategoryResource(category_list)


def _bad_request(resp, msg, *args):
    resp.body = msg % args
    # BAD REQUEST
    resp.status = falcon.HTTP_400
    return


_DB_META_FILENAME = "product-db.json"


def make_app(product_databases_dir, pipeline_path):
    log = logging.getLogger(__name__)
    #
    cat_resource = CategoryResource.read_from_json_filepath(os.path.join(product_databases_dir, _DB_META_FILENAME))
    #
    product_db_dict = {}
    for db_dir_name in cat_resource.category_dict.keys():
        db_dir_path = os.path.join(product_databases_dir, db_dir_name)
        if os.path.isdir(db_dir_path):
            try:
                pdb = EbayJsonFilesBasedProductDB(db_dir_path)
            except:
                log.error("Can't load products from %s", db_dir_path, exc_info=1)
                continue
            product_db_dict[db_dir_name] = pdb
        else:
            raise ValueError("No product directory %s" % db_dir_path)
    #
    log.info("Loading a pipeline from %s...", pipeline_path)
    pipeline = joblib.load(pipeline_path)
    log.info("Loaded the pipeline.")
    facade_dict = {
        pdb_key: ProductMatchingFacade(pdb, pipeline)
        for pdb_key, pdb in product_db_dict.items()
    }
    # build REST
    cors = CORS(
        allow_all_origins=True,
        allow_methods_list=['POST', 'GET', 'OPTIONS'],
        allow_all_headers=True
    )
    api = falcon.API(middleware=[cors.middleware])
    # api.add_route('/classify', ClassifierAnswerResource(facade))
    api.add_route('/match/{category}', ProductMatchingResource(facade_dict))
    api.add_route('/categories', cat_resource)
    return api


if __name__ == '__main__':
    arg_parser = ArgumentParser()
    arg_parser.add_argument("pipeline_path", help="a path to a serialized pipeline")
    arg_parser.add_argument("product_db_dir", help="a path to a dir with product descriptions")
    arg_parser.add_argument("--host", default='localhost')
    arg_parser.add_argument("--port", default=8080, type=int)
    arg_parser.add_argument('--log-level', default='INFO')
    args = arg_parser.parse_args()
    logging.basicConfig(level=args.log_level)
    app = make_app(args.product_db_dir, args.pipeline_path)
    #
    host, port = args.host, args.port
    log = logging.getLogger(__name__)
    server = simple_server.make_server(host, port, app)
    log.info("Serving at %s:%s" % (host, port))
    server.serve_forever()
