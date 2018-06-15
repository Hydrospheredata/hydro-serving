-- SPARK RUNTIMES

INSERT INTO hydro_serving.runtime(name, version, tags, config_params, suitable_model_types) VALUES (
'hydrosphere/serving-runtime-spark',
'2.0-latest',
'{"spark"}',
'{}',
'{"spark:2.0"}'
);

INSERT INTO hydro_serving.runtime(name, version, tags, config_params, suitable_model_types) VALUES (
'hydrosphere/serving-runtime-spark',
'2.1-latest',
'{"spark"}',
'{}',
'{"spark:2.1"}'
);

INSERT INTO hydro_serving.runtime(name, version, tags, config_params, suitable_model_types) VALUES (
'hydrosphere/serving-runtime-spark',
'2.2-latest',
'{"spark"}',
'{}',
'{"spark:2.2"}'
);

-- PYTHON RUNTIMES

INSERT INTO hydro_serving.runtime(name, version, tags, config_params, suitable_model_types) VALUES (
'hydrosphere/serving-runtime-python',
'3.6-latest',
'{"python"}',
'{}',
'{"python:3.6"}'
);

-- TENSORFLOW RUNTIMES

INSERT INTO hydro_serving.runtime(name, version, tags, config_params, suitable_model_types) VALUES (
'hydrosphere/serving-runtime-tensorflow',
'1.4.0-latest',
'{"tensorflow"}',
'{}',
'{"tensorflow:1.4.0"}'
);

INSERT INTO hydro_serving.runtime(name, version, tags, config_params, suitable_model_types) VALUES (
'hydrosphere/serving-runtime-tensorflow',
'1.5.0-latest',
'{"tensorflow"}',
'{}',
'{"tensorflow:1.5.0"}'
);

INSERT INTO hydro_serving.runtime(name, version, tags, config_params, suitable_model_types) VALUES (
'hydrosphere/serving-runtime-tensorflow',
'1.6.0-latest',
'{"tensorflow"}',
'{}',
'{"tensorflow:1.6.0"}'
);

INSERT INTO hydro_serving.runtime(name, version, tags, config_params, suitable_model_types) VALUES (
'hydrosphere/serving-runtime-tensorflow',
'1.7.0-latest',
'{"tensorflow"}',
'{}',
'{"tensorflow:1.7.0"}'
);