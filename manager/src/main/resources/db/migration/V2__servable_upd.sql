ALTER TABLE hydro_serving.servable DROP COLUMN config_params;
ALTER TABLE hydro_serving.servable DROP COLUMN cloud_driver_id;
ALTER TABLE hydro_serving.servable ADD COLUMN host TEXT;
ALTER TABLE hydro_serving.servable ADD COLUMN port INTEGER;
