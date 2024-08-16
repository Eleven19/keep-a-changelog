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
            // _         <- Console.printLine(s"**********************************************")
            // _         <- Console.printLine(s"[Changelog]: $changelog")
            // _         <- Console.printLine(s"**********************************************")
          } yield Chunk(
            test("should have the correct projectName") {
              assertTrue(changelog.projectName == Some("Changelog"))
            },
            test("should have the correct description") {
              assertTrue(
                changelog.description.get.startsWith(
                  "All notable changes to this project will be documented in this file."
                )
              )
            },
            test("should have the correct lastVersionOfKind") {
              val expected = Changelog.LastVersionOfKind(
                Some(()),
                None,
                Some(Version.parseUnsafe("1.1.1"))
              )

              val actual = changelog.lastVersionOfKind

              assertTrue(actual == expected)
            }
          )
        }
      )
    ).provideShared(ChangelogSampleDataProvider.live)

}
