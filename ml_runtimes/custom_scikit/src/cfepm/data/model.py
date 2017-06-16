DF_COL_ITEM_TITLE = 'itemtitle'
DF_COL_ITEM_SPECS = 'itemspecifics'
DF_COL_PROD_TITLE = 'epidtitle'
DF_COL_PROD_SPECS = 'productidentifier'
DF_COL_PROD_DESC = 'productdetails'


class Item:
    @staticmethod
    def product_from_row_ss(row):
        return Item(row[DF_COL_PROD_TITLE], row[DF_COL_PROD_SPECS], row[DF_COL_PROD_DESC])

    @staticmethod
    def item_from_row_ss(row):
        return Item(row[DF_COL_ITEM_TITLE], row[DF_COL_ITEM_SPECS], None)

    def __init__(self, title, spec_dict, description):
        self.title = title
        if not isinstance(spec_dict, dict):
            raise ValueError()
        self.spec_dict = spec_dict
        self.description = description
        #
        self.title_tokenized = None
        self.title_vector = None
        self.title_oov = None
        self.spec_dict_tokenized = None
        self.description_tokenized = None
        self.colour_set = None
        self.numal_set = None


def from_row_tuple(row_tuple):
    raise NotImplementedError()
    # FIXME
    # product = Product(
    #    row_tuple.epidtitle_tokenized,
    #    row_tuple.productidentifier_dict,
    #    row_tuple.productdetails_tokenized)
    # item = Item(
    #    row_tuple.itemtitle_tokenized,
    #    row_tuple.itemspecifics_dict)
    # return product, item
