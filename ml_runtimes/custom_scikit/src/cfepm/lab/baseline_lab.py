from cfepm.util.io_utils import read_dataset, ensure_parent_dir
from argparse import ArgumentParser
import sklearn.model_selection as model_selection
from cfepm.lab.validation import cross_val_score
import sklearn.preprocessing as skprep
import sklearn.linear_model as sklinear
import sklearn.metrics.scorer as sksco
from cfepm.pipeline import *
from sklearn.pipeline import Pipeline
from sklearn.externals import joblib
import logging


def prepare_feature_df(args):
    output_path = args.output_npz_path
    ft_model_path = args.fasttext_model
    input_path = args.dataset_csv_path
    min_confidence = args.min_confidence
    print("Reading a dataset from %s" % input_path)
    #
    df = _read_df(input_path, min_confidence)
    #
    pipeline_steps = []
    pipeline_steps.append(("row2bean", Row2BeanConverter()))
    pipeline_steps.append(("preprocessor", BeanTuplePreprocessor(ft_model_path)))
    pipeline_steps.append(("feature_extractor", PMFeatureExtractor()))
    pipeline = Pipeline(pipeline_steps)
    feature_arr = pipeline.transform(df)
    target_arr = df['item_yn'].values
    # write
    print("Writing the feature matrix and target vector into %s..." % output_path)
    ensure_parent_dir(output_path)
    np.savez(output_path, features=feature_arr, labels=target_arr)
    print("Done.")


def _read_df(input_path, min_confidence):
    df = read_dataset(input_path)
    if min_confidence is not None:
        print("Min confidence is set to %s. Filtering..." % min_confidence)
        len_before = len(df.index)
        df = df[df['item_yn:confidence'] >= min_confidence].copy()
        len_after = len(df.index)
        print("Removed %s rows." % (len_before - len_after))
    return df


def evaluate(args):
    verbosity = args.verbose
    with np.load(args.input_npz_path) as npz:
        feature_arr = npz['features']
        target_arr = npz['labels']
    X_arr = feature_arr
    y_arr = target_arr
    cv = model_selection.KFold(n_splits=5, shuffle=True, random_state=42777)
    # prepare feature values
    X_prep = skprep.StandardScaler()
    X_arr = X_prep.fit_transform(X_arr)
    # initialize estimator
    estimator = sklinear.LogisticRegression()
    # run CV
    scoring_tuples = [('accuracy', 'accuracy'),
                      ('yes_precision', sksco.make_scorer(sksco.precision_score, pos_label='yes')),
                      ('yes_recall', sksco.make_scorer(sksco.recall_score, pos_label='yes')),
                      ('yes_f1', sksco.make_scorer(sksco.f1_score, pos_label='yes')),
                      ('no_precision', sksco.make_scorer(sksco.precision_score, pos_label='no')),
                      ('no_recall', sksco.make_scorer(sksco.recall_score, pos_label='no')),
                      ('no_f1', sksco.make_scorer(sksco.f1_score, pos_label='no')),
                      ]
    fold_scores = cross_val_score(
        estimator, X_arr, y_arr,
        scorings=[t[1] for t in scoring_tuples],
        cv=cv, verbose=verbosity)
    fold_scores = np.vstack((fold_scores, fold_scores.mean(axis=0)))
    # print(tabulate(fold_scores,
    #                headers=[t[0] for t in scoring_tuples],
    #                showindex=['fold_' + str(i) for i in range(0, fold_scores.shape[0] - 1)] + ['Avg'],
    #                floatfmt='.4f'))


def train(args):
    input_path = args.input_path
    output_pipeline_path = args.output_pipeline_path
    min_confidence = args.min_confidence
    ft_model_path = {}#args.fasttext_model
    #
    df = _read_df(input_path, min_confidence)
    bean_tuples = Row2BeanConverter().transform(df)
    target_arr = df['item_yn'].values
    if target_arr.shape[0] != len(bean_tuples):
        raise ValueError()
    #
    pipeline_steps = []
    pipeline_steps.append(("preprocessor", BeanTuplePreprocessor(ft_model_path)))
    pipeline_steps.append(("feature_extractor", PMFeatureExtractor()))
    pipeline = Pipeline(pipeline_steps)
    print("Fitting pipeline...")
    # TODO
    pipeline.fit(bean_tuples, target_arr)
    print("Serializing into %s..." % output_pipeline_path)
    ensure_parent_dir(output_pipeline_path)
    joblib.dump(pipeline, output_pipeline_path)
    print("Done.")


if __name__ == '__main__':
    argparser = ArgumentParser("BaselineLab")
    argparser.add_argument('--log-level', default='DEBUG')
    subparsers = argparser.add_subparsers()
    # parser for prepare-feature-df subcommand
    prep_fe_df_parser = subparsers.add_parser('prepare-feature-df')
    prep_fe_df_parser.add_argument('dataset_csv_path')
    prep_fe_df_parser.add_argument('output_npz_path')
    prep_fe_df_parser.add_argument('--min-confidence', default=None, type=float)
    # prep_fe_df_parser.add_argument('--force-preprocessing', default=False, action='store_true')
    prep_fe_df_parser.add_argument('--fasttext-model', required=True)
    prep_fe_df_parser.set_defaults(func=prepare_feature_df)
    # parser for evaluate subcommand
    eval_parser = subparsers.add_parser('evaluate')
    eval_parser.add_argument('input_npz_path')
    eval_parser.add_argument('--verbose', type=int, default=1)
    eval_parser.set_defaults(func=evaluate)
    #
    train_parser = subparsers.add_parser('train')
    train_parser.add_argument('input_path')
    train_parser.add_argument('output_pipeline_path')
    train_parser.add_argument('--min-confidence', default=None, type=float)
    train_parser.set_defaults(func=train)
    # parse and run
    args = argparser.parse_args()
    logging.basicConfig(level=args.log_level)
    args.func(args)
