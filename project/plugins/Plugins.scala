import sbt._

class Plugins(info:ProjectInfo) extends PluginDefinition(info){
  val mpeltonen = "mpeltonen" at "http://github.com/mpeltonen/maven"
  val idea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.1-SNAPSHOT"
}
