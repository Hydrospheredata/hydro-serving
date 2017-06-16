from sklearn.externals import joblib
from cfepm.util.io_utils import read_dataset
from cfepm.pipeline import Row2BeanConverter

model = joblib.load('testpipe.pkl')
df = read_dataset('ebay_52K_raw_balanced.csv')
bean_tuples = Row2BeanConverter().transform(df)

print(model)
print(model.transform(bean_tuples))