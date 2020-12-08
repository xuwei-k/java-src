organization := "com.github.xuwei-k"

name := "java-src"

licenses += ("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

scalaVersion := "2.13.4"

libraryDependencies ++= (
  ("org.scala-sbt" %% "io" % "1.4.0") ::
  ("com.github.xuwei-k" %% "httpz-native" % "0.7.0") ::
  ("io.argonaut" %% "argonaut-scalaz" % "6.3.2") ::
  ("ws.unfiltered" %% "unfiltered-filter" % "0.10.0") ::
  ("javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided") ::
  Nil
)

val unusedWarnings = (
  "-Ywarn-unused" ::
  Nil
)

scalacOptions ++= (
  "-deprecation" ::
  "-unchecked" ::
  "-language:existentials" ::
  "-language:higherKinds" ::
  "-language:implicitConversions" ::
  Nil
) ::: unusedWarnings

Seq(Compile, Test).flatMap(c =>
  scalacOptions in (c, console) ~= {_.filterNot(unusedWarnings.toSet)}
)

fullResolvers ~= {_.filterNot(_.name == "jcenter")}
