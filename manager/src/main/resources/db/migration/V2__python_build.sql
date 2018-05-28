INSERT INTO hydro_serving.model_build_script(name, script) VALUES (
'python:3.6',
'FROM python:3.6-slim' || E'\n' ||
'LABEL MODEL_TYPE={MODEL_TYPE}' || E'\n' ||
'LABEL MODEL_NAME={MODEL_NAME}' || E'\n' ||
'LABEL MODEL_VERSION={MODEL_VERSION}' || E'\n' ||
'ADD {MODEL_PATH} /model' || E'\n' ||
'RUN ls /model/files/requirements.txt && pip install -r /model/files/requirements.txt --target /model/lib || echo "no requirements"' || E'\n' ||
'VOLUME /model'
);
