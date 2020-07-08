import warnings

warnings.filterwarnings('ignore', category=FutureWarning)

from keras.layers import Dense
from keras.models import Sequential
from sklearn.datasets import make_regression

# initialize data
n_samples = 1000
X, y = make_regression(n_samples=n_samples, n_features=2, noise=0.5, random_state=112)

y = y.reshape(n_samples, 1)

# create a model
model = Sequential()
model.add(Dense(15, input_dim=2, activation='relu'))
model.add(Dense(5, activation='relu'))
model.add(Dense(1, activation='linear'))

model.compile(loss='mse', optimizer='adam')
model.fit(X, y, epochs=100)

# save model
model.save('model.h5')
