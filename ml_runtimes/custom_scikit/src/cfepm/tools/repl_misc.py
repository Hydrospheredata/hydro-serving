import pandas as pd
import numpy as np
from collections import Counter
from cfepm.data.model import from_row_tuple
import re


# e.g., selector = df.item_yn == yn
def print_sample(df, selector=None, *additional_cols):
    _print_sample(df,
                  ['epidtitle_tokenized', 'itemtitle_tokenized', 'itemspecifics_dict', 'productidentifier_dict',
                   'productdetails_tokenized', 'item_yn', 'item_yn:confidence'],
                  selector, *additional_cols)


def print_sample_raw(df, selector=None, *additional_cols):
    _print_sample(df,
                  ['epidtitle', 'itemtitle', 'itemspecifics', 'productidentifier',
                   'productdetails', 'item_yn', 'item_yn:confidence'],
                  selector, *additional_cols)


def _print_sample(df, cols, selector=None, *additional_cols):
    if selector is None:
        selector = np.full((len(df.index),), True, dtype=bool)
    cols = cols + list(additional_cols)
    target_df = df[selector].sample(1)[cols]
    row = next(target_df.itertuples())
    for k, v in row._asdict().items():
        print("%s\t%s" % (k, v))


def print_image_urls(df, unit_id):
    rf = df.loc[unit_id]
    print('Item: ' + rf['itemimage'])
    print("Product: " + rf['productimage'])


def extract_spec_ss(spec_dict_ss, spec_field, remove_nones=True):
    result_ss = spec_dict_ss.apply(lambda d: d.get(spec_field) if d else None)
    if remove_nones:
        result_ss = result_ss[pd.notnull(result_ss)]
    return result_ss


def print_spec_counts(spec_col_ss, out_path):
    key_counter = Counter()
    for d in spec_col_ss:
        key_counter.update(d.keys())
    key_counter_list = list(key_counter.items())
    key_counter_list = sorted(key_counter_list, key=lambda t: t[1], reverse=True)
    with open(out_path, mode='w') as out:
        for k, c in key_counter_list:
            out.write("%s\t%s\n" % (c, k))


def write_tokens_with_num(df, out_path, logging_period=10000):
    result_counter = 0
    col_num = len(df.columns)
    with open(out_path, mode='w') as out:
        for row in df.itertuples():
            for i in range(1, col_num + 1):
                token_list = row[i]
                if token_list:
                    for t in token_list:
                        if next((ch for ch in t if str.isdigit(ch)), None):
                            out.write(t)
                            out.write('\n')
                            result_counter += 1
                            if result_counter % logging_period == 0:
                                print("%s tokens have been written" % result_counter)


_YEAR_RE = re.compile(r'(1\d|20)\d\d')
_EXPR_W_UNIT_RE = re.compile(r'(\d+\.)?\d+(cm|mm|in|tb|gb|mb|mp|ml|ghz|mhz|hz|w|v)', re.IGNORECASE)
_ORDINAL_RE = re.compile(r'\d\d?(st|nd|rd|th)')
_SHORT_NUM_RE = re.compile(r'\d{1,3}')


def _filter_tokens_with_num(input_path, output_path):
    with open(input_path) as inp:
        with open(output_path, mode='w') as out:
            for t in inp:
                t = t.rstrip()
                if _SHORT_NUM_RE.fullmatch(t):
                    continue
                if _YEAR_RE.fullmatch(t):
                    continue
                if _EXPR_W_UNIT_RE.fullmatch(t):
                    continue
                if _ORDINAL_RE.fullmatch(t):
                    continue
                out.write(t)
                out.write('\n')


def evaluate_matcher(df, matcher_func):
    fp = 0
    tp = 0
    fn = 0
    tn = 0
    yr = 0
    nr = 0
    for row in df.itertuples():
        prod, item = from_row_tuple(row)
        gold_answer = row.item_yn
        gold_answer = gold_answer == 'yes'
        sys_answer = matcher_func(prod, item)
        if gold_answer:
            if sys_answer is None:
                yr += 1
            elif sys_answer:
                tp += 1
            else:
                fn += 1
        else:
            if sys_answer is None:
                nr += 1
            elif sys_answer:
                fp += 1
            else:
                tn += 1
    acc_rec = (tp + tn) / (tp + fp + tn + fn)
    acc_overall = (tp + tn) / (tp + fp + tn + fn + yr + nr)
    recall = tp / (tp + fn)
    # positive prediction value
    precision = tp / (tp + fp)
    # negative prediction value (precision for negative)
    npv = tn / (tn + fn)
    # true negative rate (recall for negative)
    tnr = tn / (tn + fp)
    print("System \t Yes \t  No  \t Rej ")
    print("GoldYes\t%s\t%s\t%s" % (tp, fn, yr))
    print("GoldNo \t%s\t%s\t%s" % (fp, tn, nr))
    print("Accuracy (recognized): %.3f" % acc_rec)
    print("Accuracy (overall): %.3f" % acc_overall)
    print("+Recall: %.3f, +Precision: %.3f\n-Recall: %.3f, -Precision: %.3f" % (
        recall, precision, tnr, npv
    ))


def selector_has_spec_key(df, spec_col, spec_key):
    return df[spec_col].apply(lambda d: spec_key in d)


def selector_has_spec_keys_any(df, spec_col, spec_keys):
    if not isinstance(spec_keys, set):
        spec_keys = set(spec_keys)
    return df[spec_col].apply(lambda d: bool(spec_keys & d.keys()))


def extract_spec_val(df, spec_col, spec_key):
    return df[spec_col].apply(lambda d: d.get(spec_key))


def _default_token_list_flattener(tok_list):
    return ' '.join(tok_list)


def extract_distinct_value_set(df, spec_cols, spec_key, val_flattener=_default_token_list_flattener):
    if isinstance(spec_cols, str):
        spec_cols = [spec_cols]
    result_set = set()
    for col_name in spec_cols:
        ss = extract_spec_val(df, col_name, spec_key)
        ss = ss[pd.notnull(ss)]
        result_set.update(val_flattener(v) for v in ss)
    return result_set


def extract_value_counter(df, spec_cols, spec_key, val_flattener=_default_token_list_flattener):
    if isinstance(spec_cols, str):
        spec_cols = [spec_cols]
    result_counter = Counter()
    for col_name in spec_cols:
        ss = extract_spec_val(df, col_name, spec_key)
        ss = ss[pd.notnull(ss)]
        result_counter.update(val_flattener(v) for v in ss)
    return result_counter


def dump_values_per_line(iterable, outpath):
    with open(outpath, mode='w') as out:
        for v in iterable:
            out.write(v)
            out.write('\n')


def is_valid_name(s):
    return len(s) > 1 and next((c for c in s if c.isalpha()), None)


def generate_words(df, cols=("epidtitle_tokenized", "itemtitle_tokenized", "productdetails_tokenized")):
    for col in cols:
        if col not in df.columns:
            raise ValueError()
    for col in cols:
        for tokens in df[col]:
            if tokens is not None:
                for t in tokens:
                    yield t
