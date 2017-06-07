FROM mist-envoy-alpine-python-machinelearning

ENV MODEL_TYPE=type
ENV MODEL_NAME=name
ENV MODEL_VERSION=version

ENV PYTHON_START=/app/src/main.py

ENV APP_START_SCRIPT=/app/start-scikit.sh
ENV USE_APP_HTTP=true
ENV USE_APP_GRPC=false
ENV SERVICE_TYPE=serving
ENV SERVICE_NAME=localml-spark
ENV SERVICE_VERSION=0.0.1

ADD . /app/

RUN cd /app && pip install -r requirements.txt && \
	    rm -rf /var/cache/apk/* /tmp/*