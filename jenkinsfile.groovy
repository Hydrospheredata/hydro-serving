def repository = 'hydro-serving'


def buildAndPublishReleaseFunction = {
    sh "sbt compile"

    //Test
    sh "sbt test"
    sh "sbt it:testOnly"

    sh "sbt docker"

    sh "sbt paradox"
    sshagent(['hydro-site-publish']) {
        sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/target/paradox/site/main/* jenkins_publish@hydrosphere.io:serving_publish_dir"
    }
}


def buildFunction={
    sh "sbt compile docker"

    //Test
    sh "sbt test"
    sh "sbt it:testOnly"

    sh "sbt paradox"

    sh "jq"
}

def collectTestResults = {
    junit testResults: '**/target/test-reports/io.hydrosphere*.xml', allowEmptyResults: true
}

pipelineCommon(
        repository,
        false, //needSonarQualityGate,
        ["hydrosphere/serving-manager"],
        collectTestResults,
        buildAndPublishReleaseFunction,
        buildAndPublishReleaseFunction,
        buildFunction,
        null,
        "",
        "",
        {},
        commitToCD("manager", "hydrosphere/serving-manager")
)
