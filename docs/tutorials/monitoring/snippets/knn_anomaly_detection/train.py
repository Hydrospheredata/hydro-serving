import joblib


# #main-section
df = pd.read_csv("../data/adult.data", header=None)
target_labels = pd.Series(df.iloc[:, -1], index=df.index)

df = df.iloc[:, features_to_use]
df.dropna(inplace=True)

# Run feature engineering and then transformations on all features.
for feature, func in transformations.items():
    df[feature] = func(df[feature])

X_train, X_test = train_test_split(np.array(df, dtype="float"), test_size=0.2)

monitoring_model = KNN(contamination=0.05, n_neighbors=15, p = 5)
monitoring_model.fit(X_train)
# #main-section


# #plot-section
y_train_pred = monitoring_model.labels_  # binary labels (0: inliers, 1: outliers)
y_train_scores = monitoring_model.decision_scores_  # raw outlier scores

# Get the prediction on the test data
y_test_pred = monitoring_model.predict(X_test)  # outlier labels (0 or 1)
y_test_scores = monitoring_model.decision_function(X_test)  # outlier scores

plt.hist(
    y_test_scores,
    bins=30, 
    alpha=0.5, 
    density=True, 
    label="Test data outlier scores"
)
plt.hist(
    y_train_scores, 
    bins=30, 
    alpha=0.5, 
    density=True, 
    label="Train data outlier scores"
)

plt.vlines(monitoring_model.threshold_, 0, 0.1, label = "Threshold for marking outliers")
plt.gcf().set_size_inches(10, 5)
plt.legend()
# #plot-section


# #save-section
joblib.dump(monitoring_model, "../monitoring_model/monitoring_model.joblib")
# #save-section
