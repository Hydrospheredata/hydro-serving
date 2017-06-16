import pandas as pd
import os
import pkg_resources
import codecs


def read_dataset(csv_path):
    df = pd.read_csv(csv_path, index_col=0)
    # TODO remove rows where there is no values for essential product columns OR for essential item columns
    cols = list(df.columns)
    for c in {'_golden', '_unit_state', 'orig__golden', 'item_yn_gold', 'item_yn_gold_reason', 's_date'}:
        try:
            cols.remove(c)
        except:
            print("WARN: %s column not in the CSV" % c)
    return df[cols]


def ensure_parent_dir(outpath):
    os.makedirs(os.path.dirname(outpath), exist_ok=True)


def get_resource_reader(pkg_qual_name, filename):
    qpc_file = pkg_resources.resource_stream(pkg_qual_name, filename)
    utf8_reader = codecs.getreader('UTF-8')
    return utf8_reader(qpc_file)
