# Data Drift Report

Drift Report service creates a statistical report based on a comparison of training and production data distributions. It compares these two sets of data by a set of statistical tests and finds deviations.

![](../../.gitbook/assets/drift_report_screenshot%20%281%29.png)

Drift report uses multiple different tests with p=.95 for different features:

**Numerical** features:

* Levene's test with a trimmed mean 
* Welch's t-test
* Mood's test
* Kolmogorov–Smirnov test

**Categorical** features:

* Chi-Square test
* Unseen categories

## Supported Models

Right now Drift Report feature works only for Models with numerical scalar fields.

