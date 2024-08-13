package io.eleven19.keepachangelog

class ChangelogTestSuite extends munit.FunSuite {
  test("Can create a Changelog from markdown text") {
    val changelog = Changelog.fromMarkdownText(ChangelogSamples.KeepAChangelog.standard)
    assert(changelog.isRight)
  }
}
