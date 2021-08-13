# Prediction Explanation

Prediction Explanation service is designed to help Hydrosphere users understand the underlying causes of changes in predictions coming from their models.

![Tabular Explanation for class 0](../../.gitbook/assets/explanations_screenshot%20%281%29%20%284%29%20%286%29%20%281%29%20%281%29.png)

Prediction Explanation generates explanations of predictions produced by your models and tells you why a model made a particular prediction. Depending on the type of data your model uses, Prediction Explanation provides an explanation as either a set of logical predicates \(if your data is in a tabular format\) or a saliency map \(if your data is in the image format\). A saliency map is a heat map that highlights parts of a picture that a prediction was based on.

![Saliency map calculated by RISE.](../../.gitbook/assets/image%20%282%29%20%281%29%20%284%29%20%286%29%20%286%29%20%285%29.png)

Hydrosphere uses [model-agnostic](https://christophm.github.io/interpretable-ml-book/taxonomy-of-interpretability-methods.html) methods for explaining your model predictions. Such methods can be used on any machine learning model after they've been uploaded to the platform.

As of now, Hydrosphere supports explaining tabular and image data with [Anchor](https://github.com/marcotcr/anchor) and [RISE](https://github.com/eclique/RISE) tools correspondingly.

