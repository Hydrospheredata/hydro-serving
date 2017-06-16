import pandas as pd
import numpy as np
import cfepm.fe.feature_extractors as fe
import logging
from cfepm.data.model import Item, \
    DF_COL_ITEM_SPECS, DF_COL_ITEM_TITLE, DF_COL_PROD_SPECS, DF_COL_PROD_TITLE, DF_COL_PROD_DESC


class Row2BeanConverter:
    def transform(self, df):
        if not isinstance(df, pd.DataFrame):
            raise ValueError("pd.DataFrame is expected, got: %s" % type(df))
        #
        df['tmp1'] = df[DF_COL_PROD_SPECS]
        df[DF_COL_PROD_SPECS] = df['tmp1'].apply(fe.parse_specs)
        del df['tmp1']
        df['tmp1'] = df[DF_COL_ITEM_SPECS]
        df[DF_COL_ITEM_SPECS] = df['tmp1'].apply(fe.parse_specs)
        del df['tmp1']
        bean_ss = df.apply(self.convert_row_ss, axis=1)
        return list(bean_ss)

    def fit(self, X, y):
        # do nothing
        return self

    def convert_row_ss(self, row_ss):
        return Item.product_from_row_ss(row_ss), Item.item_from_row_ss(row_ss)


class DataframePreprocessor:
    def transform(self, df):
        if not isinstance(df, pd.DataFrame):
            raise ValueError("pd.DataFrame is expected, got: %s" % type(df))
        df = fe.tokenize_cols(df, DF_COL_PROD_TITLE, DF_COL_ITEM_TITLE, DF_COL_PROD_DESC)
        df = fe.parse_spec_cols(df, DF_COL_ITEM_SPECS, DF_COL_PROD_SPECS)
        df = fe.recognize_colours_df(df, 'item_colours', 'itemtitle_tokenized')
        df = fe.recognize_colours_df(df, 'prod_colours', 'epidtitle_tokenized')
        df = fe.recognize_numal_expessions(df, 'item_numals',
                                           'itemtitle_tokenized', 'itemspecifics_dict')
        df = fe.recognize_numal_expessions(df, 'prod_numals',
                                           'epidtitle_tokenized',
                                           'productidentifier_dict',
                                           'productdetails_tokenized')
        return df

    def fit(self, X, y):
        # do nothing
        return self


class BeanTuplePreprocessor:
    def __init__(self, ft_model_path):
        self.ft_model_path = ft_model_path
        #
        self._init_fasttext()

    def _init_fasttext(self):
        log = logging.getLogger(__name__)
        log.info("Not Loading FastText model from %s...", self.ft_model_path)
        self.ft_model = None
        # self.ft_model = fasttext.load_model(self.ft_model_path)
        log.info("Loaded.")

    def transform(self, bean_tuples):
        for pi_tuple in bean_tuples:
            self.preprocess_pair(*pi_tuple)
        return bean_tuples

    def preprocess_pair(self, prod, item):
        self.preprocess_single(prod)
        self.preprocess_single(item)

    def preprocess_single(self, item):
        fe.tokenize_item(item)
        #
        fe.item_recognize_colours(item)
        #
        fe.item_recognize_numal_expressions(item)
        #
        # fe.item_apply_fasttext(self.ft_model, item)

    def fit(self, X, y):
        # do nothing
        return self

    def __getstate__(self):
        state = self.__dict__.copy()
        del state['ft_model']
        return state

    def __setstate__(self, state):
        self.__dict__.update(state)
        self._init_fasttext()


class PMFeatureExtractor:
    def __init__(self):
        self._fe_list = [
            fe.SharedWordNumFE('title_shared_word_num', 'title_tokenized', 'title_tokenized'),
            fe.WordJaccardFE('title_word_jaccard', 'title_tokenized', 'title_tokenized'),
            fe.ParsedSpecFE('parsed_spec', 'spec_dict_tokenized', 'spec_dict_tokenized'),
            fe.SpecValInTitleFeatureExtractor('item_spec_vals_in_product_title',
                                              'title_tokenized', 'spec_dict_tokenized'),
            fe.SpecValInTitleFeatureExtractor('item_spec_vals_in_product_description',
                                              'description_tokenized', 'spec_dict_tokenized'),
            fe.SpecValInTitleFeatureExtractor('prod_spec_vals_in_item_title',
                                              'spec_dict_tokenized', 'title_tokenized'),
            fe.IterableColumnMismatchFeatureExtractor('title_colours_match',
                                                      'colour_set', 'colour_set'),
            fe.SetColumnJaccardIndexFE('numal_jaccard',
                                       'numal_set', 'numal_set'),
            fe.AttributeComparisonFE('attr_sim', 'spec_dict_tokenized', 'spec_dict_tokenized',
                                     fe.DEFAULT_ATTR_SIM_DICT),
            # fe.WVCosineSimilarityFE('title_ft_cosine'),
            # fe.WVPerDimDiffFE('title_ft_diff'),
            # fe.OutOfWVFE('title_ooft_jaccard'),
        ]

    def transform(self, bean_tuples):
        log = logging.getLogger(__name__)
        log.debug("Computing features...")
        fe_matrix = []
        for prod, item in bean_tuples:
            fe_list = []
            for feat_extr in self._fe_list:
                feat_extr_result = feat_extr.extract(prod, item)
                if isinstance(feat_extr_result, list):
                    fe_list += feat_extr_result
                else:
                    fe_list.append(feat_extr_result)
            fe_matrix.append(fe_list)
        # sanity check
        if len(bean_tuples) != len(fe_matrix):
            raise ValueError()
        # combine
        return np.array(fe_matrix)

    def fit(self, X, y):
        # do nothing
        return self


class FeatureDataFrame2MatrixTransformer:
    def transform(self, feature_df):
        # just a reminder
        # feature_cols = list(set(feature_df.columns) - {'target_label'})
        return feature_df.values

    def fit(self, X, y):
        # do nothing
        return self
