package io.eleven19.keepachangelog
import laika.api._
import laika.api.errors.ParserError
import laika.format.Markdown

final case class Changelog(
    projectName: Option[String],
    description: Option[String],
    entries: IndexedSeq[ChangelogEntry]
) {
  self =>

  def projectNameOrDefault(default: String): String = projectName.getOrElse(default)
  def descriptionOrDefault(default: String): String = description.getOrElse(default)
  def withSortedEntries: Changelog                  = self.copy(entries = entries.sorted)
}

object Changelog {
  val empty: Changelog = Changelog(None, None, IndexedSeq())
  def fromMarkdownText(markdownText: String): Either[ParserError, Changelog] = {
    val parserBuilder = MarkupParser.of(Markdown)
    val parser        = parserBuilder.build

    val result = parser.parse(markdownText)
    result.map { document =>
      document.sections.foreach { section =>
        println(section.title)
        section.content.foreach { content =>
          println(content)
        }

      }
      Changelog.empty

    }
  }
}
