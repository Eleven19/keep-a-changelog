package io.eleven19.keepachangelog
import zio.test._

object ChangelogSpec extends ZIOSpecDefault {

  def spec: Spec[Any, Nothing] =
    suite("ChangelogSpec")(
      suite("A Changelog")(
        suite("created from markdown text")(
          test("should have the correct projectName") {
            val result = Changelog.fromMarkdownText(ChangelogSamples.KeepAChangelog.standard)
            assertTrue(result.isRight)
          }
        )
      )
    )
}
