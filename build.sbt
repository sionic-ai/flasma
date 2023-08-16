name := "flasma"

version := "0.1"
scalaVersion := "2.12.15"

resolvers ++= Seq(
  "Local Repository" at "file://" + Path.userHome.absolutePath + "/.ivy2/local",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.7"
val LogbackVersion = "1.2.10"

libraryDependencies ++= Seq(
  "org.jetbrains.bio" % "npy" % "0.3.5",
  "org.json4s" %% "json4s-jackson" % "4.0.2",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-caching" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  "org.tensorflow" % "tensorflow-core-platform" % "0.4.1"
)

assembly / mainClass := Some("serving.http.HttpServer")
assembly / assemblyJarName := "flasma.jar"

assemblyMergeStrategy := {
  case PathList("org", "slf4j", _*) => MergeStrategy.first
  case PathList("ch", "qos", "logback", _*) => MergeStrategy.first
  case PathList("scalapb", "options", _*) => MergeStrategy.first
  case PathList("org", "objectweb", "asm", _*) => MergeStrategy.first
  case PathList("org", "apache", "commons", _*) => MergeStrategy.last
  case PathList("com", "google", _*) => MergeStrategy.last
  case PathList("org", "bytedeco", _*) => MergeStrategy.last
  case PathList(ps@_*) if ps.last.endsWith(".proto") => MergeStrategy.discard
  case PathList("META-INF", _*) => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case PathList("kubernetes", "templates", _*) => MergeStrategy.discard
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
