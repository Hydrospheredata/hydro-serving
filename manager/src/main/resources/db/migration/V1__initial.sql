CREATE TABLE hydro_serving.model
(
  model_id           BIGSERIAL PRIMARY KEY,
  name               TEXT      NOT NULL
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
  image_name        TEXT                        NOT NULL,
  image_tag         TEXT                        NOT NULL,
  image_sha256      TEXT,
  runtime_name      TEXT                        NOT NULL,
  runtime_version   TEXT                        NOT NULL,
  status            TEXT                        NOT NULL,
  profile_types     TEXT,
  install_command   TEXT,
  metadata          TEXT
);

CREATE TABLE hydro_serving.servable
(
  service_id       BIGSERIAL PRIMARY KEY,
  service_name     TEXT                                               NOT NULL UNIQUE,
  cloud_driver_id  TEXT,
  model_version_id BIGINT REFERENCES model_version (model_version_id) NOT NULL,
  status_text      TEXT                                               NOT NULL,
  config_params    TEXT                                               NOT NULL
);

CREATE TABLE hydro_serving.application
(
  id                BIGSERIAL PRIMARY KEY,
  application_name  TEXT    NOT NULL UNIQUE,
  namespace         TEXT,
  status            TEXT    NOT NULL,
  application_contract TEXT NOT NULL,
  execution_graph   TEXT    NOT NULL,
  servables_in_stage TEXT [] NOT NULL,
  kafka_streams TEXT [] NOT NULL
);