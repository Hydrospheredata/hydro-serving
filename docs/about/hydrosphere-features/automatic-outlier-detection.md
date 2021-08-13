# Automatic Outlier Detection

For each model with uploaded training data, Hydrosphere creates an outlier detection \(Auto OD\) metric, which assigns an outlier score to each request. A request is labeled as an outlier if the outlier score is greater than the 97th percentile of training data outlier scores distribution.

![](../../.gitbook/assets/auto_od_feature%20%281%29%20%284%29%20%286%29%20%286%29.gif)

You can observe those models deployed as metrics in your monitoring dashboard. These metrics provide you with information about how novel/anomalous your data is.

If these values of the metric deviate significantly from the average, you can tell that you experience a data drift and need to re-evaluate your ML pipeline to check for errors.

## Supported Models

Right now Auto OD feature works only for Models with numerical scalar fields and uploaded training data.

