from sklearn.datasets import make_regression
from hydrosdk import Cluster, Application

cluster = Cluster("http://localhost", grpc_address="localhost:9090")

app = Application.find(cluster, "linear_regression")
predictor = app.predictor()

X, _ = make_regression(n_samples=10, n_features=2, noise=0.5)
y = predictor.predict({"x": X})
print(y)