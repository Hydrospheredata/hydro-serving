ALTER TABLE hydro_serving.model_build ADD COLUMN script TEXT NOT NULL;

CREATE TABLE hydro_serving.runtime_build
(
  runtime_build_id     BIGSERIAL PRIMARY KEY,
  runtime_id           BIGINT REFERENCES runtime(runtime_id),
  started_timestamp  TIMESTAMP WITHOUT TIME ZONE        NOT NULL,
  finished_timestamp TIMESTAMP WITHOUT TIME ZONE,
  name                 TEXT                               NOT NULL,
  version              TEXT                               NOT NULL,
  tags                 TEXT [] NOT NULL,
  config_params        TEXT    NOT NULL,
  suitable_model_types TEXT [] NOT NULL DEFAULT '{"unknown"}',
  status               TEXT                               NOT NULL,
  status_text          TEXT,
  logs_url             TEXT
);