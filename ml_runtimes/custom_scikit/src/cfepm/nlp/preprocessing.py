import nltk
import pandas as pd
import ahocorasick as ac
import numpy as np

_punkt_sentence_splitter_eng = nltk.load('tokenizers/punkt/english.pickle')
_treebank_word_tokenize = nltk.tokenize.TreebankWordTokenizer().tokenize
# initialize a set of English stop-words
_stop_words = set(nltk.corpus.stopwords.words('english'))
_stop_words |= {"'s", "'ll", "n't", "'re", "'ve", "'d"}
_stop_words = frozenset(_stop_words)


def sentence_split(txt):
    if pd.isnull(txt):
        return None
    return _punkt_sentence_splitter_eng.tokenize(txt)


def _default_token_processor(s):
    if not next((ch for ch in s if ch.isalnum()), None):
        return None
    result = s.lower()
    return result if result not in _stop_words else None


def tokenize(txt, token_processor=_default_token_processor):
    if pd.isnull(txt):
        return None
    if not isinstance(txt, str):
        raise ValueError("Can't tokenize non-str value: %s" % txt)
    result_gen = (token_processor(token) for sent in sentence_split(txt)
                  for token in _treebank_word_tokenize(sent))
    return [t for t in result_gen if t]


class DictionaryMatcher(object):
    def __init__(self, *txt_file_paths, preprocessor=tokenize):
        #
        self.preprocessor = preprocessor
        # read dictionary sources
        raw_entry_list = []
        for txt_file_path in txt_file_paths:
            with open(txt_file_path) as f:
                raw_entry_list += [l.rstrip() for l in f.readlines()]
        #
        self.automaton = ac.Automaton()
        for raw_e in raw_entry_list:
            entry_norm_tokens = self._analyze(raw_e)
            # TODO optimization point
            entry_norm_key = self._to_automaton_string(entry_norm_tokens)
            self.automaton.add_word(entry_norm_key, raw_e)
        self.automaton.make_automaton()

    def contained_in(self, txt):
        if pd.notnull(txt):
            return True if next(self._get_match_iter(txt), None) else False
        else:
            return False

    def count_matches(self, txt):
        if pd.notnull(txt):
            return sum(1 for m in self._get_match_iter(txt))
        else:
            return np.nan

    # testing method
    def print_matches(self, txt):
        if pd.notnull(txt):
            tokens = self._analyze(txt)
            automaton_input = self._to_automaton_string(tokens)
            print(automaton_input)
            match_iter = self.automaton.iter(automaton_input)
            for end, matched_span in match_iter:
                print("Matched '%s' with end at %s" % (matched_span, end))
        else:
            print("! NULL input!")

    def get_matches(self, txt):
        if pd.notnull(txt):
            tokens = self._analyze(txt)
            automaton_input = self._to_automaton_string(tokens)
            match_iter = self.automaton.iter(automaton_input)
            return [matched_span for end, matched_span in match_iter]
        else:
            return []

    def replace_matches(self, txt, repl):
        if pd.notnull(txt):
            tokens = self._analyze(txt)
            automaton_input = self._to_automaton_string(tokens)
            match_iter = self.automaton.iter(automaton_input)

            def find_token_begin(token_end):
                if automaton_input[token_end] != ']':
                    raise ValueError('Token end does not point to "]"')
                for i in range(token_end, -1, -1):
                    if automaton_input[i] == '[':
                        return i
                raise ValueError("Can't find token begin")

            # an index of char after last consumed one
            last_consumed = 0
            result = ''
            for end, matched_span in match_iter:
                begin = find_token_begin(end)
                result += automaton_input[last_consumed:begin]
                result += repl
                last_consumed = end + 1
            result += automaton_input[last_consumed:]
            return result
        else:
            return txt

    def _to_automaton_string(self, tokens):
        return ' '.join(map(lambda t: '[' + t + ']', tokens))

    def _get_match_iter(self, txt):
        tokens = self._analyze(txt)
        automaton_input = self._to_automaton_string(tokens)
        return self.automaton.iter(automaton_input)

    def _analyze(self, txt):
        return self.preprocessor(txt)
