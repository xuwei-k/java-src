organization := "com.github.xuwei-k"

name := "java-src"

licenses += ("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

scalaVersion := "2.11.4"

// https://github.com/unfiltered/unfiltered/blob/v0.8.1/project/common.scala#L6
// https://github.com/unfiltered/unfiltered/blob/v0.8.2/project/common.scala#L6
// https://code.google.com/p/googleappengine/issues/detail?id=3091
libraryDependencies ++= (
  ("net.databinder" %% "unfiltered-filter" % "0.8.1") ::
  ("javax.servlet" % "servlet-api" % "2.3" % "provided") ::
  Nil
)

scalacOptions ++= (
  "-deprecation" ::
  "-unchecked" ::
  "-language:existentials" ::
  "-language:higherKinds" ::
  "-language:implicitConversions" ::
  "-Ywarn-unused" ::
  "-Ywarn-unused-import" ::
  Nil
)
