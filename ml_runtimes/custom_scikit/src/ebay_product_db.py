import logging
from cfepm.data import model

import os
import json
import logging


class ItemPersister:
    def __init__(self, output_dir):
        os.makedirs(output_dir, exist_ok=True)
        self.output_dir = output_dir
        # scan for existing files
        self._persisted_item_ids = {
            self.extract_item_id(fn) for fn in os.listdir(output_dir)
            if os.path.isfile(os.path.join(output_dir, fn)) and
               self.is_item_filename(fn)
        }
        print("There are %s items persisted in %s" % (len(self._persisted_item_ids), output_dir))
        #
        self._items_persisted_counter = 0

    def persist(self, item_dict):
        if not isinstance(item_dict, dict):
            print("WARNING: item_dict was not dict: %s" % item_dict)
            return
        item_id = str(item_dict['ItemID'])
        out_path = os.path.join(self.output_dir, self.make_item_filename(item_id))
        with open(out_path, mode='w') as out:
            json.dump(item_dict, out, indent=2)
        self._persisted_item_ids.add(item_id)
        self._items_persisted_counter += 1

    def is_persisted(self, item_id):
        if not isinstance(item_id, str):
            item_id = str(item_id)
        return item_id in self._persisted_item_ids

    def get_stats(self):
        return {
            'items_persisted': self._items_persisted_counter
        }

    @staticmethod
    def is_item_filename(fn):
        return fn.startswith('item-') and fn.endswith('.json')

    @staticmethod
    def extract_item_id(fn):
        return fn[5:-5]

    @staticmethod
    def make_item_filename(item_id):
        return 'item-%s.json' % item_id

    @staticmethod
    def load_item(file_path):
        with open(file_path) as inp:
            return json.load(inp)

    @staticmethod
    def generate_items(base_dir_path, limit=None, item_filter=None, return_source=False):
        log = logging.getLogger(__name__)
        if not os.path.isdir(base_dir_path):
            raise ValueError("%s is not an existing directory" % base_dir_path)
        #
        yield_counter = 0
        log.info("Reading products from JSON files in %s...", base_dir_path)
        for dirpath, _, filenames in os.walk(base_dir_path):
            for fn in filenames:
                if ItemPersister.is_item_filename(fn):
                    fpath = os.path.join(dirpath, fn)
                    try:
                        product = ItemPersister.load_item(fpath)
                    except Exception:
                        log.error("Can't load product from %s", fpath, exc_info=1)
                        continue
                    if item_filter and not item_filter(product):
                        continue
                    if return_source:
                        yield product, fpath
                    else:
                        yield product
                    yield_counter += 1
                    if limit and yield_counter >= limit:
                        break

def get_price(item_dict):
    price_dict = item_dict.get('CurrentPrice')
    if not price_dict:
        return None, None
    return float(price_dict.get('value')), price_dict.get('_currencyID')


def get_specs_as_dict(item_dict):
    ejson_spec_dict = item_dict.get('ItemSpecifics')
    if not ejson_spec_dict:
        return {}
    nv_list = ejson_spec_dict.get("NameValueList")
    if not nv_list:
        return {}
    if isinstance(nv_list, dict):
        # i.e., single key-value pair
        return {
            nv_list['Name']: nv_list['Value']
        }
    return {
        nv['Name']: nv['Value']
        for nv in nv_list
    }

class EbayJsonFilesBasedProductDB:
    def __init__(self, base_dir_path, limit=None):
        log = logging.getLogger(__name__)
        self.base_dir_path = base_dir_path
        # build beans
        product_list = []
        for pdict, psource in ItemPersister.generate_items(base_dir_path, limit=limit, return_source=True):
            try:
                item = to_model_item(pdict, psource)
            except Exception:
                log.error("Can't convert JSON %s into an Item object", psource, exc_info=1)
                continue
            product_list.append(item)
        log.info("Read %s products. Bulding a DataFrame...", len(product_list))
        # make immutable
        self._product_tuple = tuple(product_list)
        log.info("Done. Items: %s", len(self._product_tuple))

    @property
    def products(self):
        return self._product_tuple


def to_model_item(ebay_json_dict, source_path):
    pr_id = ebay_json_dict['ItemID']
    result = model.Item(
        ebay_json_dict.get('Title'),
        get_specs_as_dict(ebay_json_dict),
        ebay_json_dict.get('Description')
    )
    result.source_path = source_path
    result.item_id = pr_id
    result.galleryURL = ebay_json_dict.get('GalleryURL')
    result.pictureURL = _force_list(ebay_json_dict.get('PictureURL', []))
    return result


def _force_list(l):
    if isinstance(l, list):
        return l
    return [l]
