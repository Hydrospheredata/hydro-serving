import pandas as pd
import numpy as np
import re
import cfepm.nlp.preprocessing as nlp
from cfepm.util.io_utils import get_resource_reader
# from tqdm import tqdm
from collections import defaultdict
import logging

_RE_SPEC_KEY_VALUE = re.compile(r"""'(.+?)':\s+'(.+?)';""")
_log = logging.getLogger(__name__)


def parse_specs(v):
    if pd.isnull(v):
        return {}
    if isinstance(v, dict):
        d = v
        return {k.strip().lower(): _item_spec_val_to_str(v) for k, v in d.items()}
    # assume v is a string
    txt = v
    return {t[0].strip().lower(): t[1].strip() for t in _RE_SPEC_KEY_VALUE.findall(txt) if len(t) == 2}


def _item_spec_val_to_str(v):
    if v is None:
        return None
    if isinstance(v, list):
        return ', '.join(str(e) for e in v)
    if not isinstance(v, str):
        log = logging.getLogger(__name__)
        log.warning("Non-str value of item spec: %s", v)
        return str(v)
    return v


def parse_specs_and_tokenize_vals(txt):
    raw_dict = parse_specs(txt)
    return {k: nlp.tokenize(v) for k, v in raw_dict.items()}


def tokenize_cols(df, *cols):
    if not cols:
        raise ValueError("At least single column name was expected!")
    for col_name in cols:
        _log.debug("Tokenizing %s", col_name)
        df[col_name + '_tokenized'] = df[col_name].apply(nlp.tokenize)
    return df


def tokenize_item(item):
    if item.title_tokenized is None and item.title is not None:
        item.title_tokenized = nlp.tokenize(item.title)
    #if item.description_tokenized is None and item.description is not None:
    #    item.description_tokenized = nlp.tokenize(item.description)
    if item.spec_dict_tokenized is None and item.spec_dict is not None:
        item.spec_dict_tokenized = parse_specs_and_tokenize_vals(item.spec_dict)


def item_recognize_numal_expressions(item):
    if item.numal_set is None:
        token_seq_list = []
        if item.title_tokenized is not None:
            token_seq_list.append(item.title_tokenized)
        if item.description_tokenized is not None:
            token_seq_list.append(item.description_tokenized)
        if item.spec_dict_tokenized is not None:
            token_seq_list += (v for v in item.spec_dict_tokenized.values())
        item.numal_set = set(t for token_seq in token_seq_list for t in token_seq if _is_numal_expr(t))


def recognize_numal_expessions(df, target_col, *tokenized_cols):
    if not tokenized_cols:
        raise ValueError("At least single column name was expected!")
    _log.debug("Computing %s", target_col)

    def f(row_ss):
        result_set = set()
        for col in tokenized_cols:
            col_val = row_ss[col]
            if col_val is None:
                continue
            if isinstance(col_val, list):
                token_seq = col_val
                result_set.update(t for t in token_seq if _is_numal_expr(t))
            elif isinstance(col_val, dict):
                col_dict = col_val
                result_set.update(t for v in col_dict.values() for t in v if _is_numal_expr(t))
            else:
                raise ValueError("Can't handle value of type: %s" % type(col_val))
        return result_set

    result_ss = df.apply(f, axis=1)
    df[target_col] = result_ss
    return df


def _read_colour_set():
    with get_resource_reader('cfepm.nlp', 'colours.txt') as inp:
        lines = (l.strip() for l in inp)
        lines = (l for l in lines if l and not l.startswith('#'))
        return frozenset(lines)


_COLOURS = _read_colour_set()


def item_recognize_colours(item):
    if item.colour_set is None and item.title_tokenized is not None:
        item.colour_set = set(t for t in item.title_tokenized if _is_colour(t))


def recognize_colours_df(df, target_col, *tokenized_cols):
    if not tokenized_cols:
        raise ValueError("At least single column name was expected!")
    _log.debug("Computing %s", target_col)

    def f(row_ss):
        result_set = set()
        for col in tokenized_cols:
            col_val = row_ss[col]
            if col_val is None:
                continue
            if isinstance(col_val, list):
                token_seq = col_val
                result_set.update(t for t in token_seq if _is_colour(t))
            elif isinstance(col_val, dict):
                col_dict = col_val
                result_set.update(t for v in col_dict.values() for t in v if _is_colour(t))
            else:
                raise ValueError("Can't handle value of type: %s" % type(col_val))
        return result_set

    result_ss = df.apply(f, axis=1)
    df[target_col] = result_ss
    return df


def _is_numal_expr(s):
    return next((ch for ch in s if str.isdigit(ch)), None)


def _is_colour(s):
    return s in _COLOURS


def parse_spec_cols(df, *cols):
    if not cols:
        raise ValueError("At least single column name was expected!")
    for col_name in cols:
        _log.debug("Parsing specs in %s", col_name)
        df[col_name + '_dict'] = df[col_name].apply(parse_specs_and_tokenize_vals)
    return df


def _tokenize_cols_ss(ss, cols):
    result_dict = {col_name + '_tokenized': nlp.tokenize(ss[col_name]) for col_name in cols}
    return pd.Series(result_dict)


def item_apply_fasttext(ft_model, item):
    if item.title_vector is None and item.title_tokenized is not None:
        item.title_vector, item.title_oov = _text_to_vector_fasttext(ft_model, item.title_tokenized)


class FeatureExtractor:
    def __init__(self, feature_name):
        self.feature_name = feature_name

    def extract(self, item1, item2):
        pass


class TwoArgComparisonBasedFE(FeatureExtractor):
    def __init__(self, feature_name, col1_name, col2_name, none_result=0):
        super().__init__(feature_name)
        self.col1_name = col1_name
        self.col2_name = col2_name
        self.none_result = none_result

    def extract(self, item1, item2):
        v1 = getattr(item1, self.col1_name)
        if v1 is None:
            return self.none_result
        v2 = getattr(item2, self.col2_name)
        if v2 is None:
            return self.none_result
        return self.extract_by_comparison(v1, v2)

    def extract_by_comparison(self, val1, val2):
        pass


class SharedWordNumFE(TwoArgComparisonBasedFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, first_token_list, second_token_list):
        return len(set(first_token_list) & set(second_token_list))

    def __str__(self):
        return "%s: SharedWordNum b/n %s and %s" % (self.feature_name, self.col1_name, self.col2_name)


class WordJaccardFE(TwoArgComparisonBasedFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, first_token_list, second_token_list):
        if first_token_list or second_token_list:
            return jaccard_sim(set(first_token_list), set(second_token_list))
        else:
            return 0

    def __str__(self):
        return "%s: WordJaccard b/n %s and %s" % (self.feature_name, self.col1_name, self.col2_name)


class ExactlySharedSpecNumFE(TwoArgComparisonBasedFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, first_spec_dict, second_spec_dict):
        if len(first_spec_dict) <= len(second_spec_dict):
            x_dict = first_spec_dict
            y_dict = second_spec_dict
        else:
            x_dict = second_spec_dict
            y_dict = first_spec_dict
        share_count = 0
        for spec_key, x_val in x_dict.items():
            y_val = y_dict.get(spec_key)
            if x_val == y_val:
                share_count += 1
        return share_count

    def __str__(self):
        return "%s: ExactlySharedSpecNum b/n %s and %s" % (self.feature_name, self.col1_name, self.col2_name)


class ExactlySharedSpecPartFE(ExactlySharedSpecNumFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, first_spec_dict, second_spec_dict):
        shared_num = super().extract_by_comparison(first_spec_dict, second_spec_dict)
        denom = max(len(first_spec_dict), len(second_spec_dict))
        if denom == 0:
            return 0
        return shared_num / denom

    def __str__(self):
        return "%s: ExactlySharedSpecPart b/n %s and %s" % (self.feature_name, self.col1_name, self.col2_name)


class JaccardSharedSpecNumFE(TwoArgComparisonBasedFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, first_spec_dict, second_spec_dict):
        if len(first_spec_dict) <= len(second_spec_dict):
            x_dict = first_spec_dict
            y_dict = second_spec_dict
        else:
            x_dict = second_spec_dict
            y_dict = first_spec_dict
        result = 0
        for spec_key, x_val in x_dict.items():
            y_val = y_dict.get(spec_key)
            if not y_val:
                continue
            x_set = set(x_val)
            y_set = set(y_val)
            result += jaccard_sim(x_set, y_set)
        return result

    def __str__(self):
        return "%s: JaccardSharedSpecNum b/n %s and %s" % (self.feature_name, self.col1_name, self.col2_name)


class JaccardSharedSpecPartFE(JaccardSharedSpecNumFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, first_spec_dict, second_spec_dict):
        shared_num = super().extract_by_comparison(first_spec_dict, second_spec_dict)
        denom = max(len(first_spec_dict), len(second_spec_dict))
        if denom == 0:
            return 0
        return shared_num / denom

    def __str__(self):
        return "%s: JaccardSharedSpecPart b/n %s and %s" % (self.feature_name, self.col1_name, self.col2_name)


class ParsedSpecFE(TwoArgComparisonBasedFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, first_spec_dict, second_spec_dict):
        max_key_num = max(len(first_spec_dict), len(second_spec_dict))
        shared_keys = first_spec_dict.keys() & second_spec_dict.keys()
        unshared_keys = first_spec_dict.keys() ^ second_spec_dict.keys()
        if max_key_num == 0:
            # TODO most of the values here should be N/A-like
            shared_keys_num = 0
            shared_keys_part = 0
            unshared_keys_num = 0
            unshared_keys_part = 0
        else:
            shared_keys_num = len(shared_keys)
            shared_keys_part = shared_keys_num / max_key_num
            unshared_keys_num = len(unshared_keys)
            unshared_keys_part = unshared_keys_num / max_key_num
        #
        shared_exactly_counter = 0
        shared_jaccard_counter = 0
        shared_levenshtein_counter = 0
        max_val_chars = 0
        for k in shared_keys:
            x_toks = first_spec_dict.get(k)
            y_toks = second_spec_dict.get(k)
            if x_toks or y_toks:
                if x_toks == y_toks:
                    shared_exactly_counter += 1
                shared_jaccard_counter += jaccard_sim(set(x_toks), set(y_toks))
                x_joined = ' '.join(x_toks)
                y_joined = ' '.join(y_toks)
                shared_levenshtein_counter += levenshtein(x_joined, y_joined)
                max_val_chars += max(len(x_joined), len(y_joined))
        if shared_keys_num == 0:
            # TODO most of the values here should be N/A-like
            shared_exactly_num = 0
            conflicting_exactly_num = 0
            shared_exactly_part = 0
            conflicting_exactly_part = 0
            shared_jaccard_sum = 0
            conflicting_jaccard_sum = 0
            shared_jaccard_part = 0
            conflicting_jaccard_part = 0
            shared_leven_sum = 0
            shared_leven_part = 0
        else:
            shared_exactly_num = shared_exactly_counter
            conflicting_exactly_num = shared_keys_num - shared_exactly_num
            shared_exactly_part = shared_exactly_num / shared_keys_num
            conflicting_exactly_part = conflicting_exactly_num / shared_keys_num
            shared_jaccard_sum = shared_jaccard_counter
            conflicting_jaccard_sum = shared_keys_num - shared_jaccard_sum
            shared_jaccard_part = shared_jaccard_sum / shared_keys_num
            conflicting_jaccard_part = conflicting_jaccard_sum / shared_keys_num
            shared_leven_sum = shared_levenshtein_counter
            if max_val_chars > 0:
                shared_leven_part = shared_leven_sum / max_val_chars
            else:
                shared_leven_part = 0
        #
        return [
            shared_keys_num,
            shared_keys_part,
            unshared_keys_num,
            unshared_keys_part,
            shared_exactly_num,
            shared_exactly_part,
            conflicting_exactly_num,
            conflicting_exactly_part,
            shared_jaccard_sum,
            shared_jaccard_part,
            conflicting_jaccard_sum,
            conflicting_jaccard_part,
            shared_leven_sum,
            shared_leven_part,
        ]


def jaccard_sim(s1, s2):
    isect = s1 & s2
    return len(isect) / (len(s1) + len(s2) - len(isect))


def levenshtein(s1, s2):
    if len(s1) < len(s2):
        return levenshtein(s2, s1)

    # len(s1) >= len(s2)
    if len(s2) == 0:
        return len(s1)

    previous_row = range(len(s2) + 1)
    for i, c1 in enumerate(s1):
        current_row = [i + 1]
        for j, c2 in enumerate(s2):
            insertions = previous_row[
                             j + 1] + 1  # j+1 instead of j since previous_row and current_row are one character longer
            deletions = current_row[j] + 1  # than s2
            substitutions = previous_row[j] + (c1 != c2)
            current_row.append(min(insertions, deletions, substitutions))
        previous_row = current_row

    return previous_row[-1]


def extract_discriminator_word_features(train_indices, arg_df, tokenized_col1_name, tokenized_col2_name):
    df = arg_df.iloc[train_indices,
                     [arg_df.columns.get_loc(cn) for cn in ["item_yn", tokenized_col1_name, tokenized_col2_name]]]
    # TODO


class FoldDependentFeatureExtractor:
    def extract(self, df, row_i):
        pass


class DiscriminatorWordsFeatureExtractor(FoldDependentFeatureExtractor):
    def __init__(self, arg_df, train_indices, tokenized_col1_name, tokenized_col2_name, freq_threshold=3):
        df = arg_df.iloc[train_indices,
                         [arg_df.columns.get_loc(cn) for cn in ["item_yn", tokenized_col1_name, tokenized_col2_name]]]
        #
        print("Computing word discr table for cols %s and %s..." % (tokenized_col1_name, tokenized_col2_name))
        word_dict = defaultdict(DiscrCounters)
        for row_tuple in df.itertuples():
            match = row_tuple[1] == 'yes'
            a_set = _to_set_safe(row_tuple[2])
            b_set = _to_set_safe(row_tuple[3])
            shared_set = a_set & b_set
            unshared_set = a_set ^ b_set
            for w in shared_set:
                w_counter = word_dict[w]
                if match:
                    w_counter.match_shared += 1
                else:
                    w_counter.nonmatch_shared += 1
            for w in unshared_set:
                w_counter = word_dict[w]
                if match:
                    w_counter.match_unshared += 1
                else:
                    w_counter.nonmatch_unshared += 1
        print("Words before prunning: %s" % len(word_dict))
        self.word_counters = {w: w_counter for w, w_counter in word_dict.items() if w_counter.freq() >= freq_threshold}
        print("Words after prunning: %s" % len(self.word_counters))


def _to_set_safe(l):
    """
    :param l: a list or None
    :return: a set
    """
    if l is None:
        return set()
    else:
        return set(l)


class DiscrCounters:
    def __init__(self):
        self.match_shared = 0
        self.match_unshared = 0
        self.nonmatch_shared = 0
        self.nonmatch_unshared = 0

    def freq(self):
        return self.match_shared + self.match_unshared + self.nonmatch_shared + self.nonmatch_unshared

    def shared_ratio(self):
        if self.match_shared == 0:
            return 0
        if self.nonmatch_shared == 0:
            return float("inf")
        return self.match_shared / self.nonmatch_shared


class SpecValInTitleFeatureExtractor(TwoArgComparisonBasedFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, v1, v2):
        if isinstance(v1, list) and isinstance(v2, dict):
            spec_dict = v2
            txt_tokens = v1
        elif isinstance(v2, list) and isinstance(v1, dict):
            spec_dict = v1
            txt_tokens = v2
        else:
            raise ValueError()
        result_counter = 0
        for key, val_tokens in spec_dict.items():
            if contains_subseq(txt_tokens, val_tokens):
                result_counter += 1
        return result_counter


def contains_subseq(seq, sub):
    if len(sub) > len(seq):
        return False
    for i in range(0, len(seq) - len(sub) + 1):
        if seq[i:i + len(sub)] == sub:
            return True
    return False


class IterableColumnMismatchFeatureExtractor(TwoArgComparisonBasedFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, val1, val2):
        if not val1 or not val2:
            return 0
        return 1 if val1 == val2 else -1


class SetColumnJaccardIndexFE(TwoArgComparisonBasedFE):
    def __init__(self, feature_name, col1_name, col2_name):
        super().__init__(feature_name, col1_name, col2_name)

    def extract_by_comparison(self, set1, set2):
        if not set1 and not set2:
            return 0
        return jaccard_sim(set1, set2)


_NA_SPEC_VALUES = {'na', 'apply', 'none'}


def attr_val_is_na(tok_list):
    if not tok_list:
        return True
    return len(tok_list) == 1 and tok_list[0] in _NA_SPEC_VALUES


class AttributeComparisonFE(TwoArgComparisonBasedFE):
    def __init__(self, feature_name, col1_name, col2_name, attr_func_dict):
        super().__init__(feature_name, col1_name, col2_name)
        self.attr_func_dict = attr_func_dict
        self.attr_keys_sorted = sorted(attr_func_dict.keys())

    def extract_by_comparison(self, dict1, dict2):
        result_list = []
        for attr_name in self.attr_keys_sorted:
            attr_func = self.attr_func_dict[attr_name]
            v1 = dict1.get(attr_name)
            v2 = dict2.get(attr_name)
            result_list.append(self._comp_sim(v1, v2, attr_func))
        return result_list

    def _comp_sim(self, v1, v2, attr_func):
        if v1 is None or attr_val_is_na(v1):
            return 0
        if v2 is None or attr_val_is_na(v2):
            return 0
        v1 = attr_func.normalize(v1)
        v2 = attr_func.normalize(v2)
        if v1 is None or v2 is None:
            return 0
        return attr_func(v1, v2)


class ExactStringAttributeSimilarity:
    def normalize(self, tok_list):
        return ' '.join(tok_list)

    def __call__(self, v1, v2):
        if v1 == v2:
            return 1
        else:
            return -1


DEFAULT_ATTR_SIM_DICT = {
    'brand': ExactStringAttributeSimilarity(),
    'model': ExactStringAttributeSimilarity(),
    'mpn': ExactStringAttributeSimilarity(),
    'upc': ExactStringAttributeSimilarity(),
    'network': ExactStringAttributeSimilarity(),
    'publisher': ExactStringAttributeSimilarity(),
    'carrier': ExactStringAttributeSimilarity(),
    'card manufacturer': ExactStringAttributeSimilarity(),
    'year': ExactStringAttributeSimilarity(),
    'platform': ExactStringAttributeSimilarity(),
}


class FasttextVectorBasedFE(FeatureExtractor):
    def __init__(self, feature_name):
        super().__init__(feature_name)

    def extract(self, item1, item2):
        v1, oov_list_1 = item1.title_vector, item1.title_oov
        v2, oov_list_2 = item2.title_vector, item2.title_oov
        return self.extract_from_vectors(v1, v2, oov_list_1, oov_list_2)

    def extract_from_vectors(self, v1, v2, oov_list_1, oov_list_2):
        pass


def _text_to_vector_fasttext(ft_model, token_list, ret_oov_words=True):
    vec_list = []
    oov_words = []
    for t in token_list:
        if t in ft_model:
            vec_list.append(ft_model[t])
        else:
            oov_words.append(t)
    if len(vec_list) == 0:
        result_vec = np.zeros(ft_model.dim)
    else:
        result_vec = np.array(vec_list).mean(axis=0)
    return result_vec, oov_words if ret_oov_words else result_vec


class WVCosineSimilarityFE(FasttextVectorBasedFE):
    def __init__(self, feature_name):
        super().__init__(feature_name)

    def extract_from_vectors(self, v1, v2, oov_list_1, oov_list_2):
        return cosine_sim(v1, v2)


class WVPerDimDiffFE(FasttextVectorBasedFE):
    def __init__(self, feature_name):
        super().__init__(feature_name)

    def extract_from_vectors(self, v1, v2, oov_list_1, oov_list_2):
        diff_vec = v1 - v2
        return list(diff_vec)


class OutOfWVFE(FasttextVectorBasedFE):
    def __init__(self, feature_name):
        super().__init__(feature_name)

    def extract_from_vectors(self, v1, v2, oov_list_1, oov_list_2):
        if not oov_list_1:
            return 0
        oov_set1 = set(oov_list_1)
        if not oov_list_2:
            return 0
        oov_set2 = set(oov_list_2)
        return jaccard_sim(oov_set1, oov_set2)


def cosine_sim(v1, v2, fallback_val=0):
    v1_norm = np.linalg.norm(v1)
    if v1_norm == 0:
        return fallback_val
    v2_norm = np.linalg.norm(v2)
    if v2_norm == 0:
        return fallback_val
    dot_product = np.dot(v1, v2)
    return dot_product / (v1_norm * v2_norm)
