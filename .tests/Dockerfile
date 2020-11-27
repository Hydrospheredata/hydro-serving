FROM python:3.6-stretch
RUN apt-get clean && \
    apt-get update && \
    apt-get upgrade -y &&\
    apt-get install -y git
COPY requirements.txt requirements.txt
RUN pip3 install -U pip
RUN pip3 install -r requirements.txt
WORKDIR /.tests

ARG http_url=http://localhost:80
ARG grpc_url=localhost:9090
ARG grpc_secure=False
ARG cluster_name=test_cluster

ARG aws_storage_endpoint=http://localhost:9000
ARG aws_access_key_id=minio
ARG aws_secret_access_key=minio123


ENV MODELS_PATH=models
ENV CLUSTER_NAME=$cluster_name
ENV HTTP_PROXY_ADDRESS=$http_url
ENV GRPC_PROXY_ADDRESS=$grpc_url
ENV SECURE=$grpc_secure

ENV AWS_STORAGE_ENDPOINT=$aws_storage_endpoint
ENV AWS_ACCESS_KEY_ID=$aws_access_key_id
ENV AWS_SECRET_ACCESS_KEY=$aws_secret_access_key

RUN hs cluster add --name $CLUSTER_NAME --server $HTTP_PROXY_ADDRESS
RUN hs cluster use $CLUSTER_NAME
COPY . .
CMD ["sh", "run_tests.sh"]