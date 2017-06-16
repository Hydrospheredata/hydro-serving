from cfepm.util.io_utils import ensure_parent_dir
from collections import Counter
import pandas as pd

_TOKENIZED_COLS = ['epidtitle_tokenized',
                   'itemtitle_tokenized',
                   'productdetails_tokenized']

_DICT_COLS = ['itemspecifics_dict',
              'productidentifier_dict']


def write_unigram_freqs(df, output_path):
    for c in _TOKENIZED_COLS:
        if not c in df.columns:
            raise ValueError("There is no column '%s' in the given dataframe" % c)
    ensure_parent_dir(output_path)
    uni_counter = Counter()
    print("Scanning tokenized cols...")
    uni_counter.update(
        tok
        for col in _TOKENIZED_COLS
        for tok_seq in df[col] if tok_seq
        for tok in tok_seq
    )
    print("Scanning spec cols...")
    uni_counter.update(
        tok
        for col in _DICT_COLS
        for spec_dict in df[col]
        for val_toks in spec_dict.values()
        for tok in val_toks
    )
    print("There are %s tokens counted" % len(uni_counter))
    print("Sorting...")
    result_list = [tup for tup in uni_counter.items()]
    result_list = sorted(result_list, key=lambda tup: tup[1], reverse=True)
    print("Writing into %s..." % output_path)
    with open(output_path, mode='w') as out:
        for tup in result_list:
            out.write("%s\t%s\n" % tup)
    print("Done.")
