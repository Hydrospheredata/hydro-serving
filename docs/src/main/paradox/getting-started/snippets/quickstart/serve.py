import joblib

# Load model once

model = joblib.load("/model/files/model.joblib")


def infer(x):
    # Make a prediction
    y = model.predict(x)

    # Return the result
    return {"y": y.flatten()}