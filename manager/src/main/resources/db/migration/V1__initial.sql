CREATE TABLE hydro_serving.model
(
  model_id          BIGSERIAL PRIMARY KEY,
  name              TEXT      NOT NULL
);

CREATE TABLE hydro_serving.host_selector
(
  host_selector_id BIGSERIAL PRIMARY KEY,
  name           TEXT    NOT NULL UNIQUE,
  placeholder    TEXT    NOT NULL
);

CREATE TABLE hydro_serving.model_version
(
  model_version_id  BIGSERIAL PRIMARY KEY,
  model_id          BIGINT REFERENCES model (model_id) NOT NULL,
  host_selector     BIGINT REFERENCES host_selector(host_selector_id),
  created_timestamp  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  finished_timestamp TIMESTAMP WITHOUT TIME ZONE,
  model_version     BIGINT                      NOT NULL,
  model_contract    TEXT                        NOT NULL,
  model_type        TEXT                        NOT NULL,
  image_name        TEXT                        NOT NULL,
  image_tag         TEXT                        NOT NULL,
  image_sha256      TEXT                        NOT NULL,
  runtimeName       TEXT                        NOT NULL,
  runtimeVersion    TEXT                        NOT NULL,
  status            TEXT                        NOT NULL
);

CREATE TABLE hydro_serving.service
(
  service_id       BIGSERIAL PRIMARY KEY,
  service_name     TEXT                                             NOT NULL UNIQUE,
  cloud_driver_id  TEXT,
  model_version_id BIGINT REFERENCES model_version (model_version_id),
  status_text      TEXT                                             NOT NULL,
  config_params    TEXT []                                          NOT NULL
);

CREATE TABLE hydro_serving.application
(
  id                BIGSERIAL PRIMARY KEY,
  application_name  TEXT    NOT NULL UNIQUE,
  namespace         TEXT,
  application_contract TEXT NOT NULL,
  execution_graph   TEXT    NOT NULL,
  services_in_stage TEXT [] NOT NULL,
  kafka_streams TEXT [] NOT NULL
);

CREATE TABLE hydro_serving.model_build_script
(
  name   TEXT NOT NULL,
  script TEXT NOT NULL,
  PRIMARY KEY (name)
);

INSERT INTO hydro_serving.model_build_script(name, script) VALUES (
'python:3.6',
'FROM python:3.6-slim' || E'\n' ||
'LABEL MODEL_TYPE={MODEL_TYPE}' || E'\n' ||
'LABEL MODEL_NAME={MODEL_NAME}' || E'\n' ||
'LABEL MODEL_VERSION={MODEL_VERSION}' || E'\n' ||
'ADD {MODEL_PATH} /model' || E'\n' ||
'RUN ls /model/files/requirements.txt || echo "Creating empty /model/files/requirements.txt"; touch /model/files/requirements.txt' || E'\n' ||
'RUN pip install -r /model/files/requirements.txt --target /model/lib' || E'\n' ||
'VOLUME /model'
);