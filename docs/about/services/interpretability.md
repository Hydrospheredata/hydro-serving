# Interpretability

{% hint style="info" %}
Hydrosphere Interpretability is not available as an open-source solution. If you are interested in this component you can contact us via [Gitter](https://gitter.im/Hydrospheredata/hydro-serving) or our [website](https://hydrosphere.io)
{% endhint %}

Interpretability provides EDA \(Exploratory Data Analysis\) and explanations for predictions made by your models to make predictions understandable and actionable. It also produces explanations for monitoring metrics to let you know why a particular request was marked as an outlier. The component consists of 2 services: 

* Explanations
* Data Projections

Both services are built with Celery to run asynchronous tasks from apps and consists of a client, a worker, and a broker that mediates in between. A client generates a task and initiates it by adding a message to a queue, а broker delivers it to a worker, then the worker executes the task. 

Interpretability services use MongoDB as both a Celery broker and backend storage to save task results. To save and retrieve model training and production data, the Interpretability component uses S3 storage. 

When Explanation or Data Projection receives a task they create a new temporary Servable specifically for the model they need to make an explanation for. They use this Servable to run data through it in order to make new predictions and delete it after.

## Explanations

Root Cause generates explanations of model predictions to help you understand them. Depending on the type of data your model uses, it provides an explanation as either a set of logical predicates if your data is in a tabular format or a saliency map if your data is in the image format. Saliency Map is a heat map that highlights parts of a picture that a prediction was based on. 

## Data Projections

Data Projection visualizes high-dimensional data in a 2D scatter plot with an automatically trained UMAP transformer to let you evaluate data structure and spot clusters, outliers, novel data, or any other patterns. It is especially helpful if your model works with high-dimensional data, such as images or text embeddings.



