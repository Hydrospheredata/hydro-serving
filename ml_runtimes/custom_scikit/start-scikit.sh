#!/bin/sh

SERVICE_ID=$1

chmod +x $PYTHON_START

cd /app
python3 nltk_prepare_env.py

cd /app/src
exec python3 $PYTHON_START
