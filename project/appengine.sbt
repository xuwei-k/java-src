addSbtPlugin("com.eed3si9n" % "sbt-appengine" % "0.7.0")

fullResolvers ~= {_.filterNot(_.name == "jcenter")}
