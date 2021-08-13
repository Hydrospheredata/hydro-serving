# Data Projection

Data Projection is a service that **visualizes high-dimensional data in a 2D scatter plot** with an automatically trained transformer to let you evaluate the data structure and spot clusters, outliers, novel data, or any other patterns. This is especially helpful if your model works with high-dimensional data, such as images or text embeddings.

Data Projection is an important tool, which helps to describe complex things in a simple way. One good visualization can show more than text or data. Monitoring and interpretation of machine learning models are hard tasks that require analyzing a lot of raw data: training data, production requests, as well as model outputs.

Essentially, this data is just numbers that in their original form of vectors and matrices do not have any meaning since it is hard to extract any meaning from thousands of vectors of numbers. In Hydrosphere we want to make monitoring easier and clearer that is why we created a data projection service that can visualize your data in a single plot.

![](../../.gitbook/assets/data_projection_screenshot%20%281%29%20%284%29%20%286%29%20%286%29%20%281%29.png)

## Usage

To start working with Data Projection you need to create a model that has an output field with an embedding of your data. Embeddings are real-valued vectors that represent the input features in a lower dimensionality.

1. Create a model with an `embedding` field

   Data Projection service delegates the creation of embeddings to the user. It expects that model will create embedding from input features and pass it as output vector. Thus `embedding` field is required, models without this field are not supported. Data Projection also expects that output labels field is called `class` and model confidence is called respectively `confidence`. Other outputs are ignored.

2. Send data through your model
3. Check Data Projection service inside the Model Details menu

Inside Data Projection service you can see your requests features projected on a 2D space:  

![](../../.gitbook/assets/data_projector_ui_tips.png)

Each point in the plot presents a request. Requests with similar features are close to each other. You can select a specific request point and inspect what it consists of.

Above plot, there are several scores: global score, stability score, MSID score, etc. These scores reflect the quality of projection of multidimensional requests to 2D. To interpret scores you refer to technical documentation on Data Projection service.

In the **Colorize** menu, you can choose how to colorize model requests: by class, by monitoring metric or by confidence. Data Projection searchers specifically for output scalars **class** and **confidence**.

In the **Accent Points** menu, you can highlight the nearest in original space points to the selected one by picking the nearest variant. **Counterfactuals** will show you nearest points to selected but with a different predicted label.

