
# Monitoring

Every model can be monitored with a set of pre-defined and custom 
metrics. Conceptually metrics can be grouped into the following 
categories: 

- __Per-request Metrics__ — every request is evaluated against all
assigned to the model metrics. 
- __Batch Metrics__ — requests are collected into a batch and the batch 
is proceeded to the assigned metrics for the the calculation. 
- __Overall Metrics__ — metrics are calculated against all data that 
was collected during production inference. 

## Per-request Metrics

Per-request metrics allow you to make immediate judgments against an 
incoming request. Metrics described below are heavily focused on the 
anomaly detection task. We divide algorithms by the type of data that 
your models works with. 

`Numerical`

* __KNN__ — this algorithm uses distance to the nearest neighbors from 
the training dataset as a way to measure incoming sample outlier score. 

* __IsolationForest__ — this algorithm is an autoregressive stateful 
model. We fit IsolationForest on 5 consequent data samples and then 
decide whether an incoming sample is an outlier or not. 

`Image`

* __IsolationForest__ with __EfficientNet__ — this algorithm uses 
IsolationForests trained on the features, extracted from the last layer 
of the EfficientNet.

`Text`

* __Unknown Words Counter__ — this algorithm simply counts how many
unknown words are present in the observed sample.

* __KMeans__ — this algorithm uses KMeans to score each incoming sample 
against predefined clusters and decide if the observing sample is an 
outlier or not.

`All`

* __Autoencoder__ — this algorithm trains an autoencoder on the 
training/production data and shows a reconstruction error of each incoming 
sample. 

## Batch Metrics 

Batch metrics let you compute metrics in a fixed-size windows, which 
gives you an opportunity to compare distributions, validate hypothesis, 
etc. 


## Overall Metrics

Overall metrics give you an idea how all of your data looks like through 
profile calculations. Profiles, like algorithms, are differentiated by 
the data type. 

`Numerical`

* __Common statistics__ 
    - _Total request amount_
    - _Unique request amount_
    - _Unique request percentage_ 
    - _Missing values amount_
    - _Missing values percentage_

* __Quantile statistics__ 
    - _Min_
    - _5th percentile_
    - _Q1_
    - _Median_
    - _Q3_
    - _95th percentile_
    - _Range_
    - _Interquartile rage_
    - _Max_

* __Descriptive statistics__ 
    - _Standard deviation_
    - _Coefficient of variation_
    - _Kurtosis_ 
    - _Variance_
    - _Mean_
    - _Skewness_

`Text`

* __Common statistics__
    - _Mean token length_
    - _Mean character length_
    - _Mean tree depth_
    - _Mean unique lemma ratio_
    - _Mean sentiment score_
    - _Mean language proba_
    - _Mean POS proba_
