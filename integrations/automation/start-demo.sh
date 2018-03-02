WORKING_DIR=$1
SERVICE_NAME=$2
SERVICE_VERSION=$3
REDEPLOY_ALL=$4


raise_error() {
MESSAGE=$1

echo "Error occured: "${MESSAGE}
exit 1
}

[ -n "${SERVICE_VERSION}" ] || raise_error "No service version provided"

ssh-keygen -R ${TARGET_HOST} ||:
if ${REDEPLOY_ALL}; then
  ssh ubuntu@${TARGET_HOST} -C "rm -rf /home/ubuntu/cicd"
  ssh ubuntu@${TARGET_HOST} -C "mkdir -p /home/ubuntu/cicd"
  ssh ubuntu@${TARGET_HOST} -C "cd /home/ubuntu/cicd && git clone https://github.com/Hydrospheredata/hydro-serving-example.git"
fi

rsync -avz --ignore-existing ./hydro-serving/integrations ubuntu@${TARGET_HOST}:/home/ubuntu/cicd/
ssh ubuntu@${TARGET_HOST} -C "if [ ! -f /usr/bin/docker-compose ]; then sudo curl -L https://github.com/docker/compose/releases/download/1.16.1/docker-compose-`uname -s`-`uname -m` -o /usr/bin/docker-compose && sudo chmod +x /usr/bin/docker-compose; fi"

MICROSERVICE_NAME_UP=$(echo ${MICROSERVICE_NAME} | sed 's/_/-/')

case ${MICROSERVICE_VERSION} in

*.*.*)
   ssh ubuntu@${TARGET_HOST} -C "sed -i 's/${MICROSERVICE_NAME}_image.*/${MICROSERVICE_NAME}_image=hydrosphere\/serving-${MICROSERVICE_NAME_UP}/' /home/ubuntu/automation/.env"
   ssh ubuntu@${TARGET_HOST} -C "sed -i 's/${MICROSERVICE_NAME}_version.*/${MICROSERVICE_NAME}_version=${MICROSERVICE_VERSION}/' /home/ubuntu/automation/.env"
   ;;
*)
   ssh ubuntu@${TARGET_HOST} -C "eval \$(aws ecr get-login --region eu-central-1 | sed 's/-e none//')"
   ssh ubuntu@${TARGET_HOST} -C "sed -i 's/${MICROSERVICE_NAME}_image.*/${MICROSERVICE_NAME}_image=060183668755.dkr.ecr.eu-central-1.amazonaws.com\/serving-${MICROSERVICE_NAME_UP}/' /home/ubuntu/automation/.env"
   ssh ubuntu@${TARGET_HOST} -C "sed -i 's/${MICROSERVICE_NAME}_version.*/${MICROSERVICE_NAME}_version=${MICROSERVICE_VERSION}/' /home/ubuntu/automation/.env"
   ;;
esac

if ${REDEPLOY_ALL}; then
  ssh ubuntu@${TARGET_HOST} -C "cd /home/ubuntu/automation && ./destroy_all.sh ||:"
  ssh ubuntu@${TARGET_HOST} -C "cd /home/ubuntu/automation && ./start_all.sh"
else
   service=$(echo ${MICROSERVICE_NAME} | sed 's/_/-/')
   ssh ubuntu@${TARGET_HOST} -C "cd /home/ubuntu/automation && docker-compose stop ${service} && docker-compose rm -f ${service} && docker-compose up -d ${service}"

fi