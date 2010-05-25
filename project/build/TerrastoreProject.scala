import sbt._

class TerrastoreProject(info:ProjectInfo) extends DefaultProject(info) with IdeaPlugin {
  val databinder = "databinder" at "http://databinder.net/repo"

  val dispatch_http = "net.databinder" %% "dispatch-http" % "0.7.3"
  val dispatch_list_json = "net.databinder" %% "dispatch-lift-json" % "0.7.3"
  val specs = "org.scala-tools.testing" %% "specs" % "1.6.4" % "test"
}
