def repository = 'hydro-serving-helm'

def buildFunction = {
  sh "helm dependency build serving"

  // lint
  sh "rc=0; for chart in $(ls -d ./*/); do helm lint $chart || rc=$?; done; exit rc"

  // test
  sh "helm install serving --dry-run"
}

def postReleaseActionFunction = { releaseInfo ->
  sh "helm package --dependency-update --version ${releaseInfo.name}"

  def releaseFile = "serving-${releaseInfo.name}.tgz"

  uploadFilesToGithub(releaseInfo.id, releaseFile, releaseFile, repository)
}

pipelineCommon(
  repository,
  false,
  [],
  {},
  buildFunction,
  buildFunction,
  buildFunction,
  postReleaseActionFunction
)
