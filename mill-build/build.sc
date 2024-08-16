import mill._, scalalib._

object millbuild extends MillBuildRootModule {
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"de.tototec::de.tobiasroeser.mill.vcs.version_mill0.11:0.4.0"
  )
}
