from tqdm import tqdm
import cfepm.nlp.preprocessing as nlp


def apply_attribute_extractor(df, source_cols, parsed_spec_col, attr_name, val_extractor):
    if isinstance(source_cols, str):
        source_cols = [source_cols]
    tqdm.pandas(desc="Enrichment for '%s'" % attr_name)

    def _invoke_val_extractor(row_ss):
        spec_dict = row_ss[parsed_spec_col]
        attr_val = spec_dict.get(attr_name)
        if attr_val is not None:
            return attr_val
        for source_col in source_cols:
            source_val = row_ss.get(source_col)
            if source_val is not None:
                attr_val = val_extractor(source_val)
                if attr_val is not None:
                    break
        return attr_val

    return df.progress_apply(_invoke_val_extractor, axis=1)


def enrich(df, source_cols, parsed_spec_col, attr_name, val_extractor):
    val_ss = apply_attribute_extractor(df, source_cols, parsed_spec_col, attr_name, val_extractor)
    df['attr_' + attr_name] = val_ss
    return df


class DictionaryAttributeExtractor:
    def __init__(self, dict_txt_path):
        pass