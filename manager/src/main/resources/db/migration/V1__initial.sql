CREATE TABLE hydro_serving.runtime
(
  runtime_id           BIGSERIAL PRIMARY KEY,
  name                 TEXT    NOT NULL,
  version              TEXT    NOT NULL,
  tags                 TEXT [] NOT NULL,
  config_params        TEXT [] NOT NULL,
  suitable_model_types TEXT [] NOT NULL DEFAULT '{"unknown"}',
  CONSTRAINT runtime_type_name_version_unique UNIQUE (name, version)
);

CREATE TABLE hydro_serving.model
(
  model_id          BIGSERIAL PRIMARY KEY,
  name              TEXT                        NOT NULL,
  source            TEXT                        NOT NULL UNIQUE,
  description       TEXT,
  model_type        TEXT                        NOT NULL DEFAULT 'unknown',
  model_contract    TEXT                        NOT NULL DEFAULT '',
  created_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  updated_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE hydro_serving.model_version
(
  model_version_id  BIGSERIAL PRIMARY KEY,
  model_id          BIGINT REFERENCES model (model_id),
  model_version     BIGINT                      NOT NULL,
  model_name        TEXT                        NOT NULL,
  model_contract    TEXT                        NOT NULL,
  model_type        TEXT                        NOT NULL,
  source            TEXT,
  created_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  image_name        TEXT                        NOT NULL,
  image_tag         TEXT                        NOT NULL,
  image_sha256         TEXT                        NOT NULL
);

CREATE TABLE hydro_serving.model_source
(
  source_id BIGSERIAL PRIMARY KEY,
  name      TEXT NOT NULL UNIQUE,
  params    TEXT NOT NULL
);

CREATE TABLE hydro_serving.model_build
(
  model_build_id     BIGSERIAL PRIMARY KEY,
  model_id           BIGINT REFERENCES model (model_id) NOT NULL,
  model_version_id   BIGINT REFERENCES model_version (model_version_id),
  model_version      BIGINT                             NOT NULL,
  started_timestamp  TIMESTAMP WITHOUT TIME ZONE        NOT NULL,
  finished_timestamp TIMESTAMP WITHOUT TIME ZONE,
  status             TEXT                               NOT NULL,
  status_text        TEXT,
  logs_url           TEXT
);

CREATE TABLE hydro_serving.environment
(
  environment_id BIGSERIAL PRIMARY KEY,
  name           TEXT    NOT NULL UNIQUE,
  placeholders   TEXT [] NOT NULL
);

CREATE TABLE hydro_serving.service
(
  service_id       BIGSERIAL PRIMARY KEY,
  service_name     TEXT                                             NOT NULL UNIQUE,
  cloud_driver_id  TEXT,
  runtime_id       BIGINT REFERENCES runtime (runtime_id)           NOT NULL,
  environment_id   BIGINT REFERENCES environment (environment_id),
  model_version_id BIGINT REFERENCES model_version (model_version_id),
  status_text      TEXT                                             NOT NULL,
  config_params    TEXT []                                          NOT NULL
);

CREATE TABLE hydro_serving.application
(
  id                BIGSERIAL PRIMARY KEY,
  application_name  TEXT    NOT NULL UNIQUE,
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
