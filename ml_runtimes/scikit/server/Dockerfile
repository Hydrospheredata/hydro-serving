FROM frolvlad/alpine-python-machinelearning

ENV MODEL_TYPE=type
ENV MODEL_NAME=name
ENV MODEL_VERSION=version

ENV PYTHON_START=/app/src/main.py

ADD . /app/

EXPOSE 8080

WORKDIR /app

RUN pip install -r requirements.txt && \
	apk add --update curl && rm -rf /var/cache/apk/* /tmp/*

CMD ["./start.sh"]
