name := "envoy-data-plane-api"

PB.protoSources in Compile := Seq(
  baseDirectory.value / "src",
  baseDirectory.value / "ext",
  target.value / "protobuf_external"
)

PB.includePaths in Compile := Seq(
  baseDirectory.value / "src",
  baseDirectory.value / "ext",
  target.value / "protobuf_external"
)

PB.targets in Compile := Seq(
  //PB.gens.java -> (sourceManaged in Compile).value,
  scalapb.gen(
    grpc = true,
    //javaConversions=true,
    flatPackage = true
  ) -> (sourceManaged in Compile).value
)