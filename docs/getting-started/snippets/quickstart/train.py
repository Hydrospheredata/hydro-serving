import warnings

warnings.filterwarnings('ignore', category=FutureWarning)

import joblib
from sklearn.datasets import make_regression
from sklearn.linear_model import LinearRegression

# initialize data
n_samples = 1000
X, y = make_regression(n_samples=n_samples, n_features=2, noise=0.5, random_state=112)

y = y.reshape(n_samples, 1)

# create a model
model = LinearRegression()
model.fit(X, y)

joblib.dump(model, "model.joblib")
