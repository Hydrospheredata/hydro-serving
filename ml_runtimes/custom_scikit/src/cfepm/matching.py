class ExactSpecMatcher:
    def __init__(self, *keys):
        self.keys = keys

    def match(self, prod, item):
        for k in self.keys:
            p_val = prod.spec_dict.get(k)
            if not p_val:
                return None
            i_val = item.spec_dict.get(k)
            if not i_val:
                return None
            if p_val != i_val:
                return False
        return True
