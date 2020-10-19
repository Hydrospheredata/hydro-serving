# Python SDK

Python SDK offers a simple and convenient way of integrating a user's workflow scripts with Hydrosphere API.

Source code: [https://github.com/Hydrospheredata/hydro-serving-sdk](https://github.com/Hydrospheredata/hydro-serving-sdk)  
 PyPI: [https://pypi.org/project/hydrosdk/](https://pypi.org/project/hydrosdk/)

You can learn more about it in its documentation [here](https://hydrospheredata.github.io/hydro-serving-sdk/index.html).

## Installation

You can use pip to install `hydrosdk`

```bash
pip install hydrosdk
```

## Usage

You can access the locally deployed Hydrosphere platform from previous [steps](./) by running the following code:

```python
from hydrosdk import Cluster, Application 
import pandas as pd

cluster = Cluster("http://localhost", grpc_address="localhost:9090")

app = Application.find(cluster, "my-model")
predictor = app.predictor()

df = pd.read_csv("path/to/data.csv")
for row in df.itertuples(index=False):
    predictor.predict(row)
```

