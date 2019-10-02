def repository = 'hydro-serving'


if (getJobType() == "RELEASE_JOB") {
  node("JenkinsOnDemand") {

    stage("Initialize") {
      cleanWs()
      loginDockerRepository()
    }

    stage("Checkout") {
      autoCheckout(repository)
    }

    stage("Test Release") {
        sh "cd helm && helm dependency build serving"
        // lint
        sh "cd helm && rc=0; for chart in \$(ls -d ./*/); do helm lint \$chart || rc=\$?; done; return \$rc"
        // test
        sh "cd helm && helm template serving"
    }

    stage("Publish docs") {
        def curVersion = getVersion()

        sh "cd docs && sbt -DappVersion=${curVersion} paradox"
        sshagent(['hydro-site-publish']) {
            sh "cp -r ${env.WORKSPACE}/docs/target/paradox/site/main ${env.WORKSPACE}/docs/target/paradox/site/${curVersion}"
            sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/target/paradox/site/${curVersion} jenkins_publish@hydrosphere.io:serving_publish_dir_new"
            sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/src/sh jenkins_publish@hydrosphere.io:serving_scripts"
            sh "ssh -o StrictHostKeyChecking=no -t jenkins_publish@hydrosphere.io \"sh ~/serving_scripts/sh/releaseDocs.sh ${curVersion}\""
        }
    }

    stage("Create GitHub Release") {
      def curVersion = getVersion()
      def tagComment = generateTagComment()

      sh "git commit --allow-empty -a -m 'Releasing ${curVersion}'"

      writeFile file: "/tmp/tagMessage${curVersion}", text: tagComment
      sh "git tag -a ${curVersion} --file /tmp/tagMessage${curVersion}"
      sh "git checkout ${env.BRANCH_NAME}"

      sh "cd helm && helm package --dependency-update --version ${curVersion} serving"
      def releaseFile = "serving-${curVersion}.tgz"

      def sha = sh(script: "cd helm && shasum -a 256 -b ${releaseFile} | awk '{ print \$1 }'", returnStdout: true).trim()
      def sedCommand = "'s/[0-9]+\\.[0-9]+\\.[0-9]+\\/serving-[0-9]+\\.[0-9]+\\.[0-9]+\\.tgz/${curVersion}\\/serving-${curVersion}.tgz/g'"
      sh "cd helm && sed -i 'README.md' -E -e ${sedCommand} README.md"
      sh "cd helm && ./add_version.sh ${curVersion} ${sha}"
      
      sh "git commit --allow-empty -a -m 'Publishing new version ${curVersion}'"

      pushSource(repository)
      pushSource(repository, "refs/tags/${curVersion}")

      def releaseInfo = createReleaseInGithub(curVersion, tagComment, repository)
      def githubReleaseInfo = readJSON text: "${releaseInfo}"
      uploadFilesToGithub(githubReleaseInfo.id.toString(), "helm/" + releaseFile, "helm/" + releaseFile, repository)

      sh "git checkout gh-pages"
      sh "git merge master"
      pushSource(repository)
      sh "git checkout ${env.BRANCH_NAME}"
    }
  }
} else {
    node("JenkinsOnDemand") {
      stage("Initialize") {
      cleanWs()
      loginDockerRepository()
    }

    stage("Checkout") {
      autoCheckout(repository)
    }

    stage("Test Release") {
      sh "cd helm && helm dependency build serving"
      // lint
      sh "cd helm && rc=0; for chart in \$(ls -d ./*/); do helm lint \$chart || rc=\$?; done; return \$rc"
      // test
      sh "cd helm && helm template serving"
    }

    stage("Build documentation") {
      sh "cd docs && sbt -DappVersion=dev paradox"
    }

    if (env.BRANCH_NAME == "master") {
      stage("Publish documentation as dev version") {
          sshagent(['hydro-site-publish']) {
              sh "scp -o StrictHostKeyChecking=no -r ${env.WORKSPACE}/docs/target/paradox/site/main/* jenkins_publish@hydrosphere.io:serving_publish_dir_new/dev"
          }
      }
    }
  }
}