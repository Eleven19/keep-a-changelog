import mill.scalajslib.api.JsEnvConfig.JsDom
import mill.scalajslib.api.JsEnvConfig.Phantom
import mill.scalajslib.api.JsEnvConfig.Selenium
import mill.scalajslib.api.JsEnvConfig.ExoegoJsDomNodeJs
import mill.scalajslib.api.JsEnvConfig.NodeJs
import mill.codesig.JvmModel.JType.Prim.J
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.5`
import $ivy.`com.goyeau::mill-scalafix::0.3.1`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`
import $ivy.`com.carlosedp::mill-aliases::0.4.1`
import $ivy.`io.eleven19.mill::mill-crossbuild::0.3.0`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:`
import mill._, mill.scalalib._, mill.scalajslib._, mill.scalanativelib._, scalafmt._
import com.goyeau.mill.scalafix.ScalafixModule
import com.carlosedp.aliases._
import io.eleven19.mill.crossbuild._
import io.kipp.mill.ci.release.CiReleaseModule
import io.github.davidgregory084.TpolecatModule
import mill.contrib.buildinfo.BuildInfo

object keepachangelog extends Cross[KeepAChangelogModule](V.crossScalaVersions) {}

trait KeepAChangelogModule extends Cross.Module[String] with CrossPlatform {

  trait Shared extends CommonCrossProject with KeepAChangelogPublishModule {
    override def ivyDeps: T[Agg[Dep]] = Agg(
      ivy"io.kevinlee::just-semver-core::${V.`just-semver`}",
      ivy"org.typelevel::laika-core::${V.laika}"
    )
  }
  object jvm extends CommonCrossScalaJvmProject with Shared {
    object test extends TestProject with ScalaTests
  }
  object js extends CommonCrossScalaJsProject with Shared {
    object test extends TestProject with ScalaJSTests {
      def jsEnvConfig = T {
        val forkedEnv = forkEnv()
        super.jsEnvConfig() match {
          case JsDom(executable, args, env)             => JsDom(executable, args, env ++ forkedEnv)
          case Phantom(executable, args, env, autoExit) => Phantom(executable, args, env ++ forkedEnv, autoExit)
          case ExoegoJsDomNodeJs(executable, args, env) => ExoegoJsDomNodeJs(executable, args, env ++ forkedEnv)
          case NodeJs(executable, args, env, sourceMap) => NodeJs(executable, args, env ++ forkedEnv, sourceMap)
          case jsEnv                                    => jsEnv
        }
      }
    }
  }

  trait CommonCrossProject extends PlatformAwareCrossScalaProject with CrossValue with TpolecatModule
      with ScalafmtModule with ScalafixModule

  trait CommonCrossScalaJvmProject extends CrossScalaJvmProject with CommonCrossProject
  trait CommonCrossScalaJsProject extends CrossScalaJsProject with CommonCrossProject {
    def scalaJSVersion = V.scalaJS
  }

  trait TestProject extends ScalaModule with TestModule.ZioTest {

    override def forkEnv = T {
      val orig          = super.forkEnv()
      val resourcePaths = resources().map(_.path).toList
      println(s"resourcePaths: $resourcePaths")
      if (resourcePaths.isEmpty) orig
      else orig ++ Map(
        "MILL_TEST_RESOURCE_DIR"  -> resourcePaths.head.toNIO.toString(),
        "MILL_TEST_RESOURCE_DIRS" -> resourcePaths.map(_.toNIO.toString()).mkString(";")
      )
    }

    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
      ivy"dev.zio::zio-test::${V.zio}",
      ivy"dev.zio::zio-test-sbt::${V.zio}",
      ivy"com.lihaoyi::sourcecode::${V.sourcecode}"
    )
  }

}

trait KeepAChangelogPublishModule extends CiReleaseModule with JavaModule {
  import mill.scalalib.publish._
  def packageDescription =
    "Provides types and functions for working with changelog files in the Keep a Changelog format."
  def pomSettings = PomSettings(
    description = packageDescription,
    organization = "io.eleven19.keepachangelog",
    url = "https://github.com/eleven19/keep-a-changelog",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("eleven19", "keep-a-changelog"),
    developers = Seq(
      Developer("DamainReeves", "Damian Reeves", "https://github.com/damianreeves")
    )
  )
}

object V {
  val `just-semver` = "0.13.0"
  val laika         = "1.2.0"
  val sourcecode    = "0.4.2"
  val zio           = "2.1.7"

  val scala213           = "2.13.14"
  val scala3x            = "3.3.3"
  val crossScalaVersions = Seq(scala3x, scala213)

  val scalaJS = "1.16.0"
}
