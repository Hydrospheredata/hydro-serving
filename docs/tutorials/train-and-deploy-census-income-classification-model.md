# Train & Deploy Census Income Classification Model

This tutorial shows how to train and deploy a model for a classification task based on the [Adult Dataset](https://www.kaggle.com/wenruliu/adult-income-dataset). The main steps of this process are data preparation, training a model, uploading a model to the cluster, and making a prediction on test samples.

## Prerequisites

We assume that you already have a deployed instance of the Hydrosphere cloud platform and Hydro CLI installed on your local machine. If you haven't done this yet, please explore these pages first: 

{% page-ref page="../installation/" %}

{% page-ref page="../installation/cli.md" %}

For this tutorial, you can use a local cluster. To ensure that, run `hs cluster` in your terminal. This command will show the name and server address of a cluster you’re currently using. If it shows that you're not using a local cluster, you can configure one with the following commands: 

```text
hs cluster add --name local --server http://localhost
hs cluster use local
```

## Data preparation

Model training always requires some amount of initial preparation, most of which is data preparation. The Adult Dataset consists of 14 descriptors, 5 of which are numerical and 9 categorical, including the class column. 

Categorical features are usually presented as strings. This is not an appropriate data type for sending it into a model, so we need to transform it first. We can remove rows that contain question marks in some samples. Once the preprocessing is complete, you can delete the DataFrame \(`df`\): 

```python
import pandas as pd
import numpy as np
from sklearn.preprocessing import LabelEncoder  

df = pd.read_csv('adult.csv', sep = ',').replace({'?':np.nan}).dropna()

categorical_encoder = LabelEncoder()
categorical_features = ["workclass", "education", "marital-status", 
                        "occupation", "relationship", "race", "gender", 
                        "capital-gain", "capital-loss", "native-country", 'income']

numerical_features = ['age', 'fnlwgt', 'educational-num', 
                      'capital-gain', 'capital-loss', 'hours-per-week']
                        
for column in categorical_features:
    df[column] = categorical_encoder.fit_transform(df[column])

X, y = df.drop('income', axis = 1), df['income']

del df
```

## Training a model

There are many classifiers that you can potentially use for this step. In this example, we’ll apply the Random Forest classifier. After preprocessing, the dataset will be separated into train and test subsets. The test set will be used to check whether our deployed model can process requests on the cluster. After the training step, we can save a model with `joblib.dump()` in a `/model` model folder.

```python
 from sklearn.ensemble import RandomForestClassifier
 from sklearn.model_selection import train_test_split
 import joblib 
 
 train_X, test_X, train_y, test_y = train_test_split(X, y.astype(int), 
                                                    stratify=y,
                                                    test_size=0.2, 
                                                    random_state=random_seed)
 clf = RandomForestClassifier(n_estimators=20, 
                              max_depth=10,
                              n_jobs=5, 
                              random_state=random_seed).fit(train_X, train_y)
                              
 joblib.dump(clf, '/model/model.joblib')
```

## Deploy a model with SDK 

The easiest way to upload a model to your cluster is using the [Hydrosphere SDK](https://hydrosphere.gitbook.io/home/installation/sdk). SDK allows Python developers to configure and manage the model lifecycle on the Hydrosphere platform. Before uploading a model, you need to connect to your cluster: 

```python
from hydrosdk.contract import SignatureBuilder, ModelContract
from hydrosdk.cluster import Cluster

cluster = Cluster("http-cluster-address", 
                 grpc_address="grpc-cluster-address", ssl=True,
                 grpc_credentials=ssl_channel_credentials())
```

Next, we need to create an inference script to be uploaded to the Hydrosphere platform. This script will be executed each time you are instantiating a model [servable](../overview/concepts.md#servable). Let's name our function file `func_main.py`  and store it in the `src` folder inside the directory where your model is stored. Your directory structure  should look like this:

```python
.
└── model
    └── model.joblib
    └── src
        └── func_main.py
```

The code in the `func_main.py` should be as follows:

```python
import pandas as pd
from joblib import load


clf = load('/model/files/model.joblib')

cols = ['age', 'workclass', 'fnlwgt',
 'education', 'educational-num', 'marital-status',
 'occupation', 'relationship', 'race', 'gender',
 'capital-gain', 'capital-loss', 'hours-per-week',
 'native-country']

def predict(**kwargs):
    X = pd.DataFrame.from_dict({'input': kwargs}, 
                               orient='index', columns = cols)
    predicted = clf.predict(X)

    return {"y": predicted[0]}
```

It’s important to make sure that variables will be in the right order after we transform our dictionary for a prediction. So in `cols` we preserve column names as a list sorted by order of their appearance in the DataFrame.

To start working with the model in a cluster, we need to install the necessary libraries used in `func_main.py`. Create a `requirements.txt` in the folder with your model and add the following libraries to it:

```text
pandas==1.0.5
scikit-learn==0.23.2
joblib==0.16.0
```

After this, your model directory with all necessary dependencies should look as follows:

```python
.
└── model
    └── model.joblib
    └── requirements.txt
    └── src
        └── func_main.py
```

Now we are ready to upload our model to the cluster. 

Hydrosphere Serving has a strictly typed inference engine, so before uploading our model we need to specify it’s signature with`SignatureBuilder`. A [signature](../overview/concepts.md#models-signature) contains information about which method inside the `func_main.py` should be called, as well as shapes and types of its inputs and outputs. 

Use `X.dtypes` to check what types of data you have for each column. You can use `int64` fields for all variables including income, which is our dependent variable and we can name it as `'y'`  in a signature for further prediction. 

Besides, you can specify the type of profiling for each variable using `ProfilingType` so Hydrosphere could know what this variable is about and analyze it accordingly.  For this purpose, we can create a dictionary, which could contain keys as our variables and values as our profiling types. Otherwise, you can describe them one by one as a parameter in the input. 

Finally, we can complete our signature with the `.build()` method. 

```python
from hydrosdk.contract import SignatureBuilder, ModelContract, ProfilingType as PT

signature = SignatureBuilder('predict') 

col_types = {
  **dict.fromkeys(numerical_features, PT.NUMERICAL), 
  **dict.fromkeys(categorical_features, PT.CATEGORICAL)}

for i in X.columns:
    signature.with_input(i, 'int64', 'scalar', col_types[i])
    
signature = signature.with_output('y', 'int64', 'scalar', PT.NUMERICAL).build()
```

Next, we need to specify which files will be uploaded to the cluster. We use `path` to define the root model folder and `payload` to point out paths to all files that we need to upload. 

At this point, we can combine all our efforts into the `LocalModel` object. LocalModels are models before they get uploaded to the cluster. They contain all the information required to instantiate a ModelVersion in a Hydrosphere cluster. We’ll name this model `adult_model`. 

Additionally, we need to specify the environment in which our model will run. Such environments are called [Runtimes](../overview/concepts.md#runtimes). In this tutorial, we will use the default Python 3.7 runtime. This runtime uses the `src/func_main.py` script as an entry point, which is the reason we organized our files the way we did. 

One more parameter that you can define is a path to the training data of your model, required if you want to utilize additional services of Hydrosphere \(for example, [Automatic Outlier Detection](../overview/hydrosphere-features/automatic-outlier-detection.md)\). 

```python
from hydrosdk.modelversion import LocalModel
from hydrosdk.image import DockerImage

path = "model/"
payload = ['src/func_main.py', 'requirements.txt', 'model.joblib']
contract = ModelContract(predict=signature)

local_model = LocalModel(name="adult_model", 
                         install_command = 'pip install -r requirements.txt',
                         contract=contract, payload=payload,
                         runtime=DockerImage("hydrosphere/serving-runtime-python-3.7", "2.3.2", None),
                         path=path, training_data = 'data/train.csv')
```

Now we are ready to upload our model to the cluster. This process consists of several steps:

1. Once `LocalModel` is prepared we can apply the `upload` method to upload it.
2. Then we can lock any interaction with the model until it will be successfully uploaded.
3. `ModelVersion` helps to check whether our model was successfully uploaded to the platform by looking for it.



```python
from hydrosdk.modelversion import ModelVersion

uploaded_model = local_model.upload(cluster)
uploaded_model.lock_till_released()
uploaded_model.upload_training_data()

# Check that model was uploaded successfully
adult_model = ModelVersion.find(cluster, name="adult_model", 
                               version=uploaded_model.version)
```

To deploy a model you should create an [Application](https://hydrosphere.gitbook.io/home/overview/concepts#applications) - a linear pipeline of `ModelVersions` with monitoring and other benefits. Applications provide [Predictor](https://hydrospheredata.github.io/hydro-serving-sdk/hydrosdk/hydrosdk.predictor.html) objects, which should be used for data inference purposes.

```python
from hydrosdk.application import ExecutionStageBuilder, Application, ApplicationBuilder

stage = ExecutionStageBuilder().with_model_variant(adult_model).build()
app = ApplicationBuilder(cluster, "adult-app").with_stage(stage).build()
                               
predictor = app.predictor()
```

Predictors provide a `predict` method which we can use to send our data to the model. We can try to make predictions for our test set that has preliminarily been converted to a list of dictionaries. You can check the results using the name you have used for an output of Signature and preserve it in any format you would prefer. Before making a prediction don't forget to make a small pause to finish all necessary loadings.

```python
results = []
for x in test_X.to_dict('records'):
    result = predictor.predict(x)
    results.append(result['y'])
print(results[:10])
```

## Explore the UI 

If you want to interact with your model via Hydrosphere UI, you can go to `http://localhost`. Here you can find all your models. Click on a model to view information about it: versions, building logs, created applications, model's environments, and other services associated with deployed models. 

You might notice that after some time there appears an additional model with the `metric` postscript at the end of the name. This is your automatically formed monitoring model for outlier detection. Learn more about the Automatic Outlier Detection feature [here](https://hydrosphere.gitbook.io/home/overview/features/automatic-outlier-detection). 

![](../.gitbook/assets/screenshot-2020-09-14-at-17.52.30.png)

🎉 You have successfully finished this tutorial! 🎉 

## Next Steps 

Next, you can:

1. Go to the next tutorial and learn how to create a custom Monitoring Metric and attach it to your deployed model:

{% page-ref page="custom\_metric.md" %}

     2. Explore the extended part of this tutorial to learn how to use YAML resource definitions to upload a ModelVersion and create an Application.  

### Deploy a model with CLI and Resource Definitions

Another way to upload your model is to apply a [resource definition](../overview/concepts.md#resource-definitions). This process repeats all the previous steps like data preparation and training. The difference is that instead of SDK, we are using CLI to apply a resource definition. 

A [resource definition](../overview/concepts.md#resource-definitions) is a file that defines the inputs and outputs of a model, a signature function, and some other metadata required for serving. Go to the root directory of the model and create a `serving.yaml` file. You should get the following file structure:

```python
.
└── model
    └── model.joblib
    └── serving.yaml
    └── requirements.txt
    └── src
        └── func_main.py
```

Model deployment with a resource definition repeats all the steps of that with SDK, but in one file. A considerable advantage of using a resource definition is that besides describing your model it allows creating an application by simply adding an object to the contract after the separation line at the bottom. Just name your application and provide the name and version of a model you want to tie to it. 

```python
kind: Model
name: "adult_model"
payload:
  - "model/src/"
  - "model/requirements.txt"
  - "model/classification_model.joblib"
runtime: "hydrosphere/serving-runtime-python-3.6:0.1.2-rc0"
install-command: "pip install -r requirements.txt"
training-data: data/profile.csv
contract:
  name: "predict"
  inputs:
    age:
      shape: scalar
      type: int64
      profile: numerical
    workclass:
      shape: scalar
      type: int64
      profile: categorical
    fnlwgt:
      shape: scalar
      type: int64
      profile: numerical
    education:
      shape: scalar
      type: int64
      profile: categorical
    educational-num:
      shape: scalar
      type: int64
      profile: numerical
    marital_status:
      shape: scalar
      type: int64
      profile: categorical
    occupation:
      shape: scalar
      type: int64
      profile: categorical
    relationship:
      shape: scalar
      type: int64
      profile: categorical
    race:
      shape: scalar
      type: int64
      profile: categorical
    sex:
      shape: scalar
      type: int64
      profile: categorical
    capital_gain:
      shape: scalar
      type: int64
      profile: numerical
    capital_loss:
      shape: scalar
      type: int64
      profile: numerical
    hours_per_week:
      shape: scalar
      type: int64
      profile: numerical
    country:
      shape: scalar
      type: int64
      profile: categorical
  outputs:
    class:
      shape: scalar
      type: int64
      profile: numerical
---
kind: Application
name: adult_application
singular:
  model: adult_model:1
```

To start uploading, run `hs apply -f serving.yaml`. To monitor your model you can use Hydrosphere UI as was previously shown.

