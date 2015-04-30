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
  "es.bsc" % "mongoal" % "0.1.1" changing(),
  "org.apache.qpid" % "qpid-amqp-1-0-client-jms" % "0.32",
  "org.apache.qpid" % "qpid-amqp-1-0-client" % "0.32",
//  "org.apache.qpid" % "qpid-client" % "0.32",
//  "org.apache.qpid" % "qpid-jms-client" % "0.1.0",
//  "org.apache.qpid" % "qpid-common" % "0.32",
  "org.apache.qpid" % "qpid-amqp-1-0-common" % "0.32",
  "org.apache.geronimo.specs" % "geronimo-jms_1.1_spec" % "[1.1.1,)"
)
