CREATE TABLE hydro_serving.runtime_type
(
  runtime_type_id BIGSERIAL PRIMARY KEY,
  name            TEXT NOT NULL,
  version         TEXT NOT NULL,
  CONSTRAINT runtime_type_name_version_unique UNIQUE (name, version)
);

CREATE TABLE hydro_serving.model
(
  model_id          BIGSERIAL PRIMARY KEY,
  name              TEXT                        NOT NULL,
  source            TEXT                        NOT NULL UNIQUE,
  runtime_type_id   BIGINT REFERENCES runtime_type (runtime_type_id),
  output_fields     TEXT []                     NOT NULL,
  input_fields      TEXT []                     NOT NULL,
  description       TEXT,
  created_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  updated_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE hydro_serving.model_runtime
(
  runtime_id        BIGSERIAL PRIMARY KEY,
  runtime_type_id   BIGINT REFERENCES runtime_type (runtime_type_id),
  modelName         TEXT                        NOT NULL,
  modelVersion      TEXT                        NOT NULL,
  source            TEXT                        ,
  output_fields     TEXT []                     NOT NULL,
  input_fields      TEXT []                     NOT NULL,
  created_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  image_name        TEXT                        NOT NULL,
  image_tag         TEXT                        NOT NULL,
  image_md5_tag     TEXT                        NOT NULL,
  model_id          BIGINT REFERENCES model (model_id)
);

CREATE TABLE hydro_serving.model_build
(
  model_build_id     BIGSERIAL PRIMARY KEY,
  model_id           BIGINT REFERENCES model (model_id) NOT NULL,
  model_version      TEXT                               NOT NULL,
  started_timestamp  TIMESTAMP WITHOUT TIME ZONE        NOT NULL,
  finished_timestamp TIMESTAMP WITHOUT TIME ZONE,
  status             TEXT                               NOT NULL,
  status_text        TEXT,
  logs_url           TEXT,
  runtime_id         BIGINT REFERENCES model_runtime (runtime_id)
);

CREATE TABLE hydro_serving.model_service
(
  service_id      BIGSERIAL PRIMARY KEY,
  service_name    TEXT                                         NOT NULL UNIQUE,
  cloud_driver_id TEXT,
  runtime_id      BIGINT REFERENCES model_runtime (runtime_id) NOT NULL,
  status          TEXT,
  statusText      TEXT
);

CREATE TABLE hydro_serving.pipeline
(
  pipeline_id BIGSERIAL PRIMARY KEY,
  name        TEXT    NOT NULL UNIQUE,
  stages      TEXT [] NOT NULL
);

CREATE TABLE hydro_serving.endpoint
(
  endpoint_id   BIGSERIAL PRIMARY KEY,
  endpoint_name TEXT                                     NOT NULL UNIQUE,
  pipeline_id   BIGINT REFERENCES pipeline (pipeline_id)
);

CREATE TABLE hydro_serving.runtime_type_build_script
(
  name            TEXT NOT NULL,
  version         TEXT,
  script          TEXT NOT NULL,
  PRIMARY KEY (name, version)
)