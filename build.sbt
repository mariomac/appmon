name := """application-monitor"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

resolvers += "MongoAL at GitHub" at "https://raw.githubusercontent.com/mariomac/MongoAL/master/mvn-repo"


libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "org.mongodb" % "mongo-java-driver" % "[3.0,)",
  "es.bsc" % "mongoal" % "0.1.1" changing()
  //"org.apache.qpid" % "proton-j" % "0.8"
)
