# Drift Report

{% hint style="info" %}
Drift Report is not available as an open-source solution. If you are
interested in this component you can contact us via [gitter](https://gitter.im/Hydrospheredata/hydro-serving)
or our [website](https://hydrosphere.io)
{% endhint %}

Drift Report service creates a statistical report based on a comparison of training and
production data distributions. It compares these two sets of data by a set of statistical
tests and finds deviations. 

![](./images/drift_report_screenshot.png)

Drift report uses multiple different tests with p=.95 for different features:
 
__Numerical__ features:

* Levene's test with a trimmed mean 
* Welch's t-test
* Mood's test
* Kolmogorov–Smirnov test

__Categorical__ features:

* Chi-Square test
* Unseen categories
