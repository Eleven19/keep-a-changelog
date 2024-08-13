package io.eleven19.keepachangelog
import zio._
import zio.test._

object ChangelogSpec extends ZIOSpecDefault {

  def spec: Spec[Any, Throwable] =
    suite("ChangelogSpec")(
      suite("A Changelog")(
        suite("created from markdown text") {
          for {
            changelogContents <-
              Live.live(
                ChangelogSampleDataProvider.standardSampleContent
              ) // NOTE: In order to get the real environment variables we need to use Live.live
            changelog <- ZIO.fromEither(Changelog.fromMarkdownText(changelogContents))
          } yield Chunk(
            test("should have the correct projectName") {
              assertTrue(changelog.projectName == Some("Changelog"))
            },
            test("should have the correct description") {
              assertTrue(
                changelog.description == Some("All notable changes to this project will be documented in this file.")
              )
            }
          )
        }
      )
    ).provideShared(ChangelogSampleDataProvider.live)
}
