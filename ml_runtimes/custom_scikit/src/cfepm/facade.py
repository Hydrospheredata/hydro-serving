import logging
import numpy as np
import pandas as pd
from cfepm.data import model
import time
from cfepm.pipeline import BeanTuplePreprocessor
import multiprocessing
from concurrent.futures import ThreadPoolExecutor

ITEM_TITLE_KEY = 'Title'
ITEM_SPECS_KEY = 'ItemSpecifics'


class ProductMatchingFacade:
    def __init__(self, product_db, pipeline, max_workers=None):
        self.product_db = product_db
        self.pipeline = pipeline
        # preprocess products
        self._preprocess_db()
        # index of 'yes' class in a pipeline classifier
        #self._yes_i = list(pipeline.steps[-1][1].classes_).index('yes')
        # setup executor
        if not max_workers or max_workers < 1:
            max_workers = multiprocessing.cpu_count()
        self.partition_num = max_workers
        self._executor = ThreadPoolExecutor(max_workers)

    def _preprocess_db(self):
        log = logging.getLogger(__name__)
        bt_processor = next(st for st in self.pipeline.named_steps.values() if isinstance(st, BeanTuplePreprocessor))
        progress = 0
        for item in self.product_db.products:
            bt_processor.preprocess_single(item)
            progress += 1
            if progress % 1000 == 0:
                log.info("%s items have been preprocessed...", progress)

    def classify_pairs_df(self, bean_tuples):
        # predict
        prediction_arr = self.pipeline.predict_proba(bean_tuples)
        #return prediction_arr[:, self._yes_i]
        return prediction_arr

    def get_top_matches(self, item_dict, min_prob=0.7, top_n=10):
        if top_n < 1:
            return []
        log = logging.getLogger(__name__)
        item = model.Item(item_dict[ITEM_TITLE_KEY], item_dict.get(ITEM_SPECS_KEY), None)
        bean_tuples = [(prod, item) for prod in self.product_db.products]
        pairs_num = len(bean_tuples)
        time_before = time.time()

        results = self.pipeline.transform(bean_tuples)
        time_after = time.time()
        time_took = (time_after - time_before) * 1000
        time_per_row = time_took / pairs_num
        log.debug("Classification took %s ms for %s pairs, per pair: %s ms" %
                  (time_took, pairs_num, time_per_row))

        return results
        # if top_matched_scores.shape[0] != len(top_matched_products):
        #     raise ValueError()
        # #
        # time_after = time.time()
        # time_took = (time_after - time_before) * 1000
        # time_per_row = time_took / pairs_num
        # log.debug("Classification took %s ms for %s pairs, per pair: %s ms" %
        #           (time_took, pairs_num, time_per_row))
        # # sort and convert to std lists
        # sorted_indices = np.argsort(top_matched_scores)[::-1]
        # sorted_indices = sorted_indices[:top_n]
        # top_matched_scores = list(top_matched_scores[sorted_indices])
        # top_matched_products = list(top_matched_products[i] for i in sorted_indices)
        # result_list = [self._to_response_dict(prod, score)
        #                for prod, score in zip(top_matched_products, top_matched_scores)
        #                if score >= min_prob]
        # return result_list

    def _to_response_dict(self, item, score):
        return {
            'Title': item.title,
            'Specs': item.spec_dict,
            'Description': item.description,
            'MatchProbability': score,
            'GalleryURL': getattr(item, 'galleryURL', None),
            'PictureURL': getattr(item, 'pictureURL', None)
        }

    def _get_top_matches_on_partition(self, bean_tuples, top_n):
        pairs_num = len(bean_tuples)
        match_prob_arr = self.classify_pairs_df(bean_tuples)
        if match_prob_arr.shape[0] != pairs_num:
            raise ValueError("Unexpected result prob array length: expected - %s, got - %s" %
                             (pairs_num, match_prob_arr.shape[0]))
        # get top n
        top_matched_indices = np.argpartition(match_prob_arr, -top_n)[-top_n:]
        top_matched_scores = match_prob_arr[top_matched_indices]
        top_matched_products = list(bean_tuples[i][0] for i in top_matched_indices)
        return top_matched_scores, top_matched_products


def _filter_nan(v):
    if isinstance(v, list):
        return v
    return None if pd.isnull(v) else v


def chunks(l, chunk_num):
    """Yield n chunks from l."""
    n = int(len(l) / chunk_num)
    if n * chunk_num < len(l):
        n += 1
    for i in range(0, len(l), n):
        yield l[i:i + n]
