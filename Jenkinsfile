node("JenkinsOnDemand") {
    stage('Clone project ') {
        checkout scm
        sh "cd ${env.WORKSPACE}"
    }

    stage('Build and test') {
        sh "${env.WORKSPACE}/sbt/sbt -Dsbt.override.build.repos=true -Dsbt.repository.config=${env.WORKSPACE}/project/repositories clean compile test docker"
    }
}