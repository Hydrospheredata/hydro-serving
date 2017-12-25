ALTER TABLE hydro_serving.runtime_type ADD COLUMN model_type TEXT NOT NULL DEFAULT 'unknown';

DELETE FROM hydro_serving.runtime_type;
INSERT INTO hydro_serving.runtime_type (name, version, model_type, tags, config_params) VALUES
  ('hydrosphere/serving-runtime-dummy', '0.0.1', 'unknown', '{"python","code","test"}', '{}'),
  ('hydrosphere/serving-runtime-tensorflow', '0.0.1', 'tensorflow', '{"tensorflow","python","ml"}', '{}'),
  ('hydrosphere/serving-runtime-sparklocal-2.0', '0.0.1', 'spark:2.0', '{"spark:2.0","scala","ml"}', '{}'),
  ('hydrosphere/serving-runtime-sparklocal-2.1', '0.0.1', 'spark:2.1', '{"spark:2.1","scala","ml"}', '{}'),
  ('hydrosphere/serving-runtime-sparklocal-2.2', '0.0.1', 'spark:2.2', '{"spark:2.2","scala","ml"}', '{}'),
  ('hydrosphere/serving-runtime-py2databricks', '0.0.1', 'python:2.7', '{"python2","databricks","sk-learn", "ml"}', '{}'),
  ('hydrosphere/serving-runtime-scikit', '0.0.1', 'scikit', '{"scikit","ml"}', '{}');

ALTER TABLE hydro_serving.model DROP COLUMN output_fields;
ALTER TABLE hydro_serving.model DROP COLUMN input_fields;
ALTER TABLE hydro_serving.model DROP COLUMN runtime_type_id;
ALTER TABLE hydro_serving.model ADD COLUMN model_type TEXT NOT NULL DEFAULT 'unknown';
ALTER TABLE hydro_serving.model ADD COLUMN model_contract TEXT NOT NULL DEFAULT '';

ALTER TABLE hydro_serving.model_runtime DROP COLUMN input_fields;
ALTER TABLE hydro_serving.model_runtime DROP COLUMN output_fields;
ALTER TABLE hydro_serving.model_runtime ADD COLUMN model_contract TEXT NOT NULL DEFAULT '';

ALTER TABLE hydro_serving.model_build ADD COLUMN runtime_type_id BIGINT REFERENCES hydro_serving.runtime_type;