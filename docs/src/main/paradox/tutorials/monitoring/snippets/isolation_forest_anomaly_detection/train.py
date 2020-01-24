import joblib

# #main-section
df = pd.read_csv("../data/taxi_pickups.csv")
df.set_index(pd.to_datetime(df.pickup_datetime),inplace=True)
df.drop(["pickup_datetime"], axis=1, inplace=True)

data, _ = transform_to_sliding_windows(df)
iforest = IsolationForest(
    n_jobs=-1, random_state=42,  behaviour="new", contamination=0.03)
is_outlier = iforest.fit_predict(data)
# Find outliers in training data 
outlier_indices = df.index[6:][is_outlier==-1]
# #main-section


# #plot-section
plt.plot(df.index, df.pickups, label="Training data")
plt.vlines(outlier_indices, 0, 600, colors="red", alpha=0.2, label="Outliers")

plt.gcf().set_size_inches(25, 5)
plt.legend()
# #plot-section


# #save-section
joblib.dump(iforest, '../monitoring_model/iforest.joblib')
# #save-section