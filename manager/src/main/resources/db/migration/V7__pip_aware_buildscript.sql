UPDATE hydro_serving.model_build_script SET script =
'FROM python:3.6-slim' || E'\n' ||
'LABEL MODEL_TYPE={MODEL_TYPE}' || E'\n' ||
'LABEL MODEL_NAME={MODEL_NAME}' || E'\n' ||
'LABEL MODEL_VERSION={MODEL_VERSION}' || E'\n' ||
'ADD {MODEL_PATH} /model' || E'\n' ||
'RUN ls /model/files/requirements.txt || echo "Creating empty /model/files/requirements.txt"; touch /model/files/requirements.txt' || E'\n' ||
'RUN pip install -r /model/files/requirements.txt' || E'\n' ||
'VOLUME /model'
WHERE name='python:3.6';