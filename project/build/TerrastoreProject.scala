import sbt._

class TerrastoreProject(info: ProjectInfo) extends DefaultProject(info) with IdeaPlugin {
  val databinder = "databinder" at "http://databinder.net/repo"
  val scalaToolsSnapshots = ScalaToolsSnapshots

  val dispatch_http = "net.databinder" %% "dispatch-http" % "0.7.4"
  val dispatch_list_json = "net.databinder" %% "dispatch-lift-json" % "0.7.4"
  val specs = "org.scala-tools.testing" %% "specs" % "1.6.5-SNAPSHOT" % "test"

  import Process._
  import FileUtilities._
  import java.io.File
  import java.net.URL

  /*
  terrastore config
   */
  def terrastoreVersion: String = "0.5.1"

  def terrastoreInstallPath: Path = "terrastore" / terrastoreVersion

  def terrastoreURL: URL = new URL("http://terrastore.googlecode.com/files/terrastore-" + terrastoreVersion + "-dist.zip")

  def terrastoreMasterPath = terrastoreInstallPath / "single-master"

  def terrastoreServerPath = terrastoreInstallPath / "server"

  /*
  tasks
   */

  lazy val terrastoreDownload = terrastoreDownloadAction

  lazy val terrastoreInstallMaster = terrastoreInstallMasterAction

  lazy val terrastoreInstallServer = terrastoreInstallServerAction

  lazy val terrastoreInstall = task{ None } dependsOn(terrastoreInstallMaster, terrastoreInstallServer) describedAs "installs terrastore master & server" 

  lazy val terrastoreRemoveServer = terrastoreRemoveServerAction

  lazy val terrastoreRemoveMaster = terrastoreRemoveMasterAction

  lazy val terrastoreRemove = task{ None } dependsOn(terrastoreRemoveMaster, terrastoreRemoveServer) describedAs "removes terrastore master & server"

  /*
  actions
   */

  def terrastoreDownloadAction = task {

    def downloadTo(file:File) = download(terrastoreURL, file, log)

    def mkDir = createDirectory(terrastoreInstallPath, log)

    def downloadAndUnzip = withTemporaryFile(log, "download", "zip") {
      file =>
        (downloadTo(file) orElse mkDir).map(Left(_)) getOrElse
        unzip(file, terrastoreInstallPath, log)
    }.left.toOption

    if(terrastoreInstallPath.exists)
      None
    else{
      downloadAndUnzip
    }

  } describedAs "Downloads terrastore"

  def terrastoreInstallAction(kind:Path) = task {
    if(kind.exists)
      None
    else {
      val cmd = "ant -f %s %s -Dinstall.dir=%s".format(
        terrastoreInstallPath ** "terrastore-install.xml" absString,
        kind.name,
        kind.absolutePath)
      (cmd ! log) match {
        case 0 => None
        case n => Some("status code: "+n)
      }
    }
  } dependsOn terrastoreDownload

  def terrastoreInstallMasterAction =
    terrastoreInstallAction(terrastoreMasterPath).describedAs("installs terrastore master into " + terrastoreMasterPath)

  def terrastoreInstallServerAction =
    terrastoreInstallAction(terrastoreServerPath).describedAs("installs terrastore server into " + terrastoreServerPath)

  def terrastoreRemoveMasterAction =
    task{ FileUtilities.clean(terrastoreMasterPath, log) } describedAs "removes "+terrastoreMasterPath

  def terrastoreRemoveServerAction =
    task{ FileUtilities.clean(terrastoreServerPath, log) } describedAs "removes "+terrastoreServerPath
}
