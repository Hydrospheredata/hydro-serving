FROM python:3.6-stretch
RUN apt-get clean && \
    apt-get update && \
    apt-get upgrade -y &&\
    apt-get install -y git
COPY requirements.txt requirements.txt
RUN pip3 install -U pip
RUN pip3 install -r requirements.txt
WORKDIR /.tests
ENV MODELS_PATH=models
ENV CLUSTER_NAME=test_cluster
ENV HTTP_PROXY_ADDRESS=http://localhost:80
ENV GRPC_PROXY_ADDRESS=localhost:9090
ENV SECURE=False

ENV AWS_STORAGE_ENDPOINT=http://localhost:9000
ENV AWS_ACCESS_KEY_ID=minio
ENV AWS_SECRET_ACCESS_KEY=minio123

RUN hs cluster add --name $CLUSTER_NAME --server $HTTP_PROXY_ADDRESS
RUN hs cluster use $CLUSTER_NAME
COPY . .
CMD ["sh", "run_tests.sh"]