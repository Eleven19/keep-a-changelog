import mill._, scalalib._

val millVersion = "0.11.11"
object millbuild extends MillBuildRootModule {
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"de.tototec::de.tobiasroeser.mill.vcs.version_mill0.11:0.4.0",
    ivy"com.lihaoyi::mill-contrib-sonatypecentral:${millVersion}"
  )
}
