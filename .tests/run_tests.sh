python -m pytest -s  model_upload_test.py --tx 2*popen --dist=loadscope -p no:warnings
python -m pytest -s  training_data_test.py  --tx 2*popen --dist=loadscope -p no:warnings
python -m pytest -s  application_creation_test.py  --tx 2*popen --dist=loadscope -p no:warnings
python -m pytest -s  feature_lake_test.py --tx 2*popen --dist=loadscope -p no:warnings