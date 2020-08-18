# Monitoring



{% hint style="info" %}
Hydrosphere Monitoring is not available as an open-source solution. If you are interested in this component you can contact us via [gitter](https://gitter.im/Hydrospheredata/hydro-serving) or our [website](https://hydrosphere.io)
{% endhint %}

## Automatic Outlier Detection

Hydrosphere Monitoring includes monitoring every request going through your platform. Every request is analyzed by an automatically generated based on training data provided outlier detection model.

![](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/services/monitoring/images/auto_od_metric.png)

You can observe those models deployed as metrics in your monitoring dashboard. These metrics provide you with information about how novel/anomalous your data is.

If these values of this metric deviate significantly from the average, you can tell that you experience a data drift and need to re-evaluate your ML pipeline to check for errors.

## Sonar

Sonar service is responsible for managing metrics, training and production data storage, calculating profiles, and shadowing data to the Model Versions which are used as an outlier detection metrics.

![](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/services/monitoring/images/monitoring_screenshot.png)



## Drift Report



Drift Report service creates a statistical report based on a comparison of training and production data distributions. It compares these two sets of data by a set of statistical tests and finds deviations.

![](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/services/monitoring/images/drift_report_screenshot.png)

Drift report uses multiple different tests with p=.95 for different features:

**Numerical** features:

* Levene's test with a trimmed mean 
* Welch's t-test
* Mood's test
* Kolmogorov–Smirnov test

**Categorical** features:

* Chi-Square test
* Unseen categories

