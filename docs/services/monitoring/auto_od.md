# Automatic Outlier Detection

{% hint style="info" %} Automatic Outlier Detection is not available as
an open-source solution. If you are interested in this component you can
contact us via [gitter](https://gitter.im/Hydrospheredata/hydro-serving)
or our [website](https://hydrosphere.io) {% endhint %}

Hydrosphere Monitoring includes monitoring every request going through
your platform. Every request is analyzed by an automatically generated
based on training data provided outlier detection model.

![](./images/auto_od_metric.png)

You can observe those models deployed as metrics in your monitoring
dashboard. These metrics provide you with information about how
novel/anomalous your data is.

If these values of this metric deviate significantly from the average,
you can tell that you experience a data drift and need to re-evaluate
your ML pipeline to check for errors.
