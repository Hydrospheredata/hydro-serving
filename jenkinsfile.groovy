def repository = 'hydro-serving'


def buildAndPublishReleaseFunction={
    //Buid serving
    def curVersion = currentVersion()
    sh "sbt -DappVersion=${curVersion} compile docker"

    //Buid docs
    sh "sbt -DappVersion=${curVersion} makeMicrosite"

    //Test
    sh "sbt -DappVersion=${curVersion} test"
    sh "sbt -DappVersion=${curVersion} it:testOnly"


    sh "sbt docs/makeMicrosite"
    sh "jekyll build --source ${env.WORKSPACE}/docs/target/site --destination ${env.WORKSPACE}/docs/target/site/_site"
    sshagent(['hydro-site-publish']) {
        sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/target/site/_site/* jenkins_publish@hydrosphere.io:serving_publish_dir"
    }
}


def buildFunction={
    //Buid serving
    def curVersion = currentVersion()
    sh "sbt -DappVersion=${curVersion} compile docker"

    //Buid docs
    sh "sbt -DappVersion=${curVersion} makeMicrosite"

    //Test
    sh "sbt -DappVersion=${curVersion} test"
    sh "sbt -DappVersion=${curVersion} it:testOnly"
}

def collectTestResults = {
    junit testResults: '**/target/test-reports/io.hydrosphere*.xml', allowEmptyResults: true
}

pipelineCommon(
        repository,
        false, //needSonarQualityGate,
        ["hydrosphere/serving-manager", "hydrosphere/serving-runtime-dummy"],
        collectTestResults,
        buildAndPublishReleaseFunction,
        buildAndPublishReleaseFunction,
        buildFunction
)