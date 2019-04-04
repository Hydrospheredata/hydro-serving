def repository = 'hydro-serving-helm'

def buildFunction = {
  sh "helm dependency build serving"

  // lint
  sh "rc=0; for chart in \$(ls -d ./*/); do helm lint \$chart || rc=\$?; done; return \$rc"

  // test
  sh "helm template serving"
}

if (getJobType() == "RELEASE_JOB") {
  node("JenkinsOnDemand") {

    stage("Initialize") {
      cleanWs()
      loginDockerRepository()
    }

    stage("Checkout") {
      autoCheckout(repository)
    }

    stage('Set release version') {
      def curVersion = getVersion()
      def releaseVersion = mapVersionToRelease(curVersion)
      sh "git checkout -b release_temp"
      setVersion(releaseVersion)
    }

    stage("Test Release") {
      buildFunction()
    }

    stage("Create GitHub Release") {
      def curVersion = getVersion()
      def tagComment = generateTagComment()

      sh "git commit -a -m 'Releasing ${curVersion}'"

      writeFile file: "/tmp/tagMessage${curVersion}", text: tagComment
      sh "git tag -a ${curVersion} --file /tmp/tagMessage${curVersion}"
      sh "git checkout ${env.BRANCH_NAME}"

      sh "helm package --dependency-update --version ${curVersion} serving"
      def releaseFile = "serving-${curVersion}.tgz"

      def sha = sh(script: "shasum -a 256 -b ${releaseFile} | awk '{ print \$1 }'", returnStdout: true).trim()
      def sedCommand = "'s/[0-9]+\\.[0-9]+\\.[0-9]+\\/serving-[0-9]+\\.[0-9]+\\.[0-9]+\\.tgz/${curVersion}\\/serving-${curVersion}.tgz/g'"
      sh "sed -i 'README.md' -E -e ${sedCommand} README.md"
      sh "./add_version.sh ${curVersion} ${sha}"

      def nextVersion = mapReleaseVersionToNextDev(curVersion)
      setVersion(nextVersion)
      sh "git commit -a -m 'Development version increased: ${nextVersion}'"

      pushSource(repository)
      pushSource(repository, "refs/tags/${curVersion}")

      def releaseInfo = createReleaseInGithub(curVersion, tagComment, repository)
      def githubReleaseInfo = readJSON text: "${releaseInfo}"
      uploadFilesToGithub(githubReleaseInfo.id.toString(), releaseFile, releaseFile, repository)

      sh "git checkout gh-pages"
      sh "git merge master"
      pushSource(repository)
      sh "git checkout ${env.BRANCH_NAME}"
    }
  }
} else {
  pipelineCommon(
    repository,
    false,
    [],
    {},
    buildFunction,
    buildFunction,
    buildFunction
  )
}
