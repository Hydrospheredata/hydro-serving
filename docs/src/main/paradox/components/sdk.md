# SDK

The purpose of this SDK is to provide a simple and convenient way of 
integrating user's workflow scripts with Serving API. It is built on
top of existing CLI code. 

Source code: https://github.com/Hydrospheredata/hydro-serving-sdk<br>
PyPI: https://pypi.org/project/hydrosdk/

## Installation 

```sh
pip install hydrosdk
```

## Usage

```python
from hydrosdk import sdk

# Build the model
model = sdk.Model()
model.with_name("model")
model.with_runtime(
    "hydrosphere/serving-runtime-python-3.6:0.1.2-rc0")
model.with_signature(
    sdk.Signature('predict') \
        .with_input('imgs', 'float32', [-1, 28, 28, 1], 'image') \
        .with_output('probabilities', 'float32', [-1, 10]) \
        .with_output('class_ids', 'int64', [-1, 1]) \
    )
model.with_metadata({
    "architecture": "arm",
    "dataset": "s3://bucket/data/version=jd382998asd0993209asd1",
})
model.with_payload(
    ["requirements.txt", "src", "model.joblib"])
model.with_install_command(
    "pip install -r requirements.txt")
model.with_training_data(
    "s3://bucket/data/version=jd382998asd0993209asd1")

# Apply the model
result = model.apply(hydrosphere_uri)
```