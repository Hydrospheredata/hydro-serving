FROM mist-envoy-alpine-python-machinelearning

ENV PYTHON_START=/app/src/main.py

ENV APP_START_SCRIPT=/app/start-scikit.sh
ENV USE_APP_HTTP=true
ENV USE_APP_GRPC=false
ENV SERVICE_TYPE=serving
ENV SERVICE_NAME=customscikit
ENV SERVICE_VERSION=0.0.1

RUN apk update
RUN ln -s locale.h /usr/include/xlocale.h && \
    apk add gcc linux-headers gfortran musl-dev python3-dev
ADD . /app/

RUN cd /app && pip install -r requirements.txt && \
	    rm -rf /var/cache/apk/* /tmp/*
