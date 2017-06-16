"""
Extended routines of Sklearn validation modules
"""
from sklearn.utils.validation import indexable
from sklearn.model_selection._validation import _index_param_value, _score, _safe_split
from sklearn.model_selection import check_cv
from sklearn.metrics.scorer import check_scoring
from sklearn.base import is_classifier, clone
from sklearn.externals.joblib import Parallel, delayed
import numpy as np


def cross_val_score(estimator, X, y=None, fold_specific_X_extractor=None,
                    groups=None, scorings=None, cv=None,
                    n_jobs=1, verbose=0, fit_params=None,
                    pre_dispatch='2*n_jobs'):
    """
    :param estimator: 
    :param X: 
    :param y: 
    :param fold_specific_X_extractor: 
    :param groups: 
    :param scorings: list of scorings (strings, callables, etc...)
    :param cv: 
    :param n_jobs: 
    :param verbose: 
    :param fit_params: 
    :param pre_dispatch: 
    :return: an array of scores, shape: <folds x scores>
    """
    X, y, groups = indexable(X, y, groups)

    cv = check_cv(cv, y, classifier=is_classifier(estimator))
    scorers = [check_scoring(estimator, scoring=scoring) for scoring in scorings]
    # We clone the estimator to make sure that all the folds are
    # independent, and that it is pickle-able.
    parallel = Parallel(n_jobs=n_jobs, verbose=verbose,
                        pre_dispatch=pre_dispatch)
    scores = parallel(delayed(_fe_fit_and_score)(clone(estimator), X, y, scorers,
                                                 train, test, verbose, None,
                                                 fit_params, fold_specific_X_extractor=fold_specific_X_extractor)
                      for train, test in cv.split(X, y, groups))
    # here scores is python list of shape <folds x 1 x scores>
    scores = np.array(scores)
    # eliminate middle axis
    return scores.reshape((scores.shape[0], scores.shape[2]))


def _fe_fit_and_score(estimator, X, y, scorers, train, test, verbose,
                      parameters, fit_params, return_train_score=False,
                      return_parameters=False, fold_specific_X_extractor=None):
    if verbose > 1:
        if parameters is None:
            msg = ''
        else:
            msg = '%s' % (', '.join('%s=%s' % (k, v)
                                    for k, v in parameters.items()))
        print("[CV] %s %s" % (msg, (64 - len(msg)) * '.'))

    # Adjust length of sample weights
    fit_params = fit_params if fit_params is not None else {}
    fit_params = dict([(k, _index_param_value(X, v, train))
                       for k, v in fit_params.items()])

    if parameters is not None:
        estimator.set_params(**parameters)

    X_train, y_train = _safe_split(estimator, X, y, train)
    X_test, y_test = _safe_split(estimator, X, y, test, train)
    if fold_specific_X_extractor:
        # extend by fold-specific features
        X_train_additional = fold_specific_X_extractor(train)
        if X_train_additional is not None:
            X_train = np.concatenate([X_train, X_train_additional], axis=1)
    #
    if y_train is None:
        estimator.fit(X_train, **fit_params)
    else:
        estimator.fit(X_train, y_train, **fit_params)
    #
    test_scores = [_score(estimator, X_test, y_test, scorer) for scorer in scorers]
    if return_train_score:
        train_scores = [_score(estimator, X_train, y_train, scorer) for scorer in scorers]
    #
    if verbose > 2:
        msg += ", scores=%s" % test_scores
    if verbose > 1:
        print("[CV] %s %s" % ((64 - len(msg)) * '.', msg))

    ret = [train_scores, test_scores] if return_train_score else [test_scores]

    if return_parameters:
        ret.append(parameters)
    return ret
