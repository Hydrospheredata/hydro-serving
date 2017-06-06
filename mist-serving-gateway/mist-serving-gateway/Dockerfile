FROM nginx:1.13-alpine

ENV TEMPLATE_OUTPUT_NAME=/etc/nginx/conf.d/default.conf
ENV TEMPLATE_FILE_NAME=/app/default.ctmpl
ENV CONSUL_VERSION=0.18.3


ADD consul.json /app/
ADD start.sh /app/
ADD default.ctmpl /app/

RUN apk add --update curl \
    && curl -O https://releases.hashicorp.com/consul-template/${CONSUL_VERSION}/consul-template_${CONSUL_VERSION}_linux_386.tgz \
    && tar -xvzf consul-template_${CONSUL_VERSION}_linux_386.tgz \
    && mv consul-template /usr/local/bin/ \
    && rm consul-template_${CONSUL_VERSION}_linux_386.tgz \
    && rm -rf /var/cache/apk/* /tmp/*

CMD ["app/start.sh"]
