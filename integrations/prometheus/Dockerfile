FROM prom/prometheus:v1.7.1

ADD customEntryPoint.sh /bin/
ADD curl-7.30.0.ermine.tar.bz2 /tmp
RUN mv /tmp/curl-7.30.0.ermine/curl.ermine /bin/curl \
    && rm -Rf /tmp/curl-7.30.0.ermine

ENTRYPOINT [ "/bin/customEntryPoint.sh" ]