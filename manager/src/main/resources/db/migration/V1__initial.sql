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

CREATE TABLE hydro_serving.model_build
(
  model_build_id     BIGINT PRIMARY KEY,
  model_id           BIGINT REFERENCES model (model_id),
  model_version      TEXT                        NOT NULL,
  started_timestamp  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  finished_timestamp TIMESTAMP WITHOUT TIME ZONE,
  status             TEXT                        NOT NULL,
  status_text        TEXT,
  logs_url           TEXT
);

CREATE TABLE hydro_serving.runtime
(
  runtime_id        BIGINT PRIMARY KEY,
  model_build_id    BIGINT REFERENCES model_build (model_build_id),
  runtime_type_id   BIGINT REFERENCES runtime_type (runtime_type_id),
  modelName         TEXT                        NOT NULL,
  modelVersion      TEXT                        NOT NULL,
  source            TEXT                        NOT NULL,
  output_fields     TEXT [],
  input_fields      TEXT [],
  created_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  image_name        TEXT                        NOT NULL,
  image_tag         TEXT                        NOT NULL,
  image_md5_tag     TEXT                        NOT NULL
);