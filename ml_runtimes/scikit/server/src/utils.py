import numpy as np
import pandas as pd

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