def repository = 'hydro-serving'


def buildAndPublishReleaseFunction = {
    def curVersion = getVersion()

    sh "cd manager && sbt -DappVersion=${curVersion} compile"

    sh "cd manager && sbt -DappVersion=${curVersion} test"
    sh "cd manager && sbt -DappVersion=${curVersion} it:testOnly"

    sh "cd manager && sbt -DappVersion=${curVersion} docker"

    sh "cd docs && sbt -DappVersion=${curVersion} paradox"
    sshagent(['hydro-site-publish']) {
        sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/target/paradox/site/main/* jenkins_publish@hydrosphere.io:serving_publish_dir_new/${curVersion}"
				sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/src/sh/releaseDocs.sh jenkins_publish@hydrosphere.io"
				sh "ssh -o StrictHostKeyChecking=no -t jenkins_publish@hydrosphere.io \"sh ~/releaseDocs.sh ${curVersion}\""
    }
}


def buildMasterFunction = {
    def curVersion = getVersion()

    sh "cd manager && sbt -DappVersion=${curVersion} compile"

    sh "cd manager && sbt -DappVersion=${curVersion} test"
    sh "cd manager && sbt -DappVersion=${curVersion} it:testOnly"

    sh "cd manager && sbt -DappVersion=${curVersion} docker"

    sh "cd docs && sbt -DappVersion=dev paradox"

    sshagent(['hydro-site-publish']) {
        sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/target/paradox/site/main/* jenkins_publish@hydrosphere.io:serving_publish_dir_new/dev"
    }
}

def buildFunction = {
    def curVersion = getVersion()

    sh "cd manager && sbt -DappVersion=${curVersion} compile"

    sh "cd manager && sbt -DappVersion=${curVersion} test"
    sh "cd manager && sbt -DappVersion=${curVersion} it:testOnly"

    sh "cd manager && sbt -DappVersion=${curVersion} docker"

    sh "cd docs && sbt -DappVersion=dev paradox"
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
        buildMasterFunction,
        buildFunction,
        null,
        "",
        "",
        {},
        commitToCD("manager", "hydrosphere/serving-manager")
)
