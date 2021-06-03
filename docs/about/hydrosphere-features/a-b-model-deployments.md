# A/B Model Deployments

Hydrosphere allows you to A/B test your ML models in production.

A/B testing is a great way of measuring how well your models perform or which of your model versions is more effective and taking data-driven decisions upon this knowledge.

Production ML applications always have specific goals, for example driving as many users as possible to perform some action. To achieve these goals, it’s necessary to run online experiments and compare model versions using metrics in order to measure your progress against them. This approach allows to track whether your development efforts lead to desired outcomes.

To perform a basic A/B experiment on an application consisting of 2 variants of a model, you need to train and upload both versions to Hydrosphere, create an application with a single execution stage from them, invoke it by simulating production data flow, then analyze production data using metrics of your choice.

Learn how to set up an A/B application:

* [A/B Analysis for a Recommendation Model](https://docs.hydrosphere.io/quickstart/tutorials/a-b-analysis-for-a-recommendation-model)

