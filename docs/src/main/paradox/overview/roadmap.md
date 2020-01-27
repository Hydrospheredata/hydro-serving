# Roadmap 

This page describes our roadmap of feature incorporation into the platform.

### API and integrations

- External models support
    - Abstract model registration and metrics ingestion API
    - SageMaker models tailored integration
- Model instrumentation SDK 
- Kubeflow upgrades and integrations 
- Metrics egress API
- Alerting

### Monitoring
- Auto Outlier Detection [Tabular]
    - HBOS with predefined parameters
    - Hyperparameter candidates based on historical data & model
    - HBOS with grid search 
    - Add LOOP and IsolationForest algorithms
    - Add algorithm suggestion based on historical data 
    - Add more algorithms [TBD]
- High-Dimensional Visualization
    - Make UMAP visualization with predefined parameters on data
    - Add anomaly-wise and class-wise coloring of visualized data
    - UMAP Grid Search for optimal hyperparameters
    - Add continuous anomaly-wise and class-wise colorings
    - Add counterfactuals and closest neighbours
    - Add ability to isolate subset of points
    - Add image support
- Batch-Checks
    - Monitoring batches of tabular data with set of researched metrics
    - Add support for image data type
- Root Cause 
    - Performance fixes
