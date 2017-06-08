FROM mist-envoy-alpine-python-machinelearning

ENV PYTHON_START=/app/src/main.py

ENV APP_START_SCRIPT=/app/start-scikit.sh
ENV USE_APP_HTTP=true
ENV USE_APP_GRPC=false
ENV SERVICE_TYPE=serving
ENV SERVICE_NAME=scikit
ENV SERVICE_VERSION=0.0.1

ADD . /app/

RUN cd /app && pip install -r requirements.txt && \
	    rm -rf /var/cache/apk/* /tmp/*