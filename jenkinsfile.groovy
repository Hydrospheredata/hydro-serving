def repository = 'hydro-serving'


def buildAndPublishReleaseFunction = {
    def curVersion = getVersion()

    sh "sbt compile"

    //Test
    sh "sbt test"
    sh "sbt it:testOnly"

    sh "sbt docker"

    sh "sbt paradox"
    sshagent(['hydro-site-publish']) {
        sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/target/paradox/site/main/* jenkins_publish@hydrosphere.io:serving_publish_dir_new/${curVersion}"
        sh "ssh -o StrictHostKeyChecking=no -t jenkins_publish@hydrosphere.io \"sudo ln -Fs ~/serving_docs_new/${curVersion} ~/serving_docs_new/latest\""
        sh "ssh -o StrictHostKeyChecking=no -t jenkins_publish@hydrosphere.io \"sudo cp ~/serving_docs_new/latest/paradox.json ~/serving_docs_new/paradox.json\""
        sh "ssh -o StrictHostKeyChecking=no -t jenkins_publish@hydrosphere.io \"jq '.[. | length] |= . + \"${curVersion}\"' ~/serving_docs_new/versions.json > ~/serving_docs_new/versions_new.json\""
        sh "ssh -o StrictHostKeyChecking=no -t jenkins_publish@hydrosphere.io \"mv ~/serving_docs_new/versions_new.json  ~/serving_docs_new/versions.json\""
    }
}


def buildFunction={
    sh "sbt compile docker"

    //Test
    sh "sbt test"
    sh "sbt it:testOnly"

    sh "sbt -DappVersion=dev paradox"

    sshagent(['hydro-site-publish']) {
        sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/target/paradox/site/main/* jenkins_publish@hydrosphere.io:serving_publish_dir_new/dev"
    }
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
