package io.eleven19.keepachangelog
import errors.ParsingError
import laika.api._
import laika.ast._
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

  def fromDocument(document: Document): Either[ParsingError, Changelog] = {
    var first                       = true
    var projectName: Option[String] = None
    val description: Option[String] = None
    document.sections.foreach { section =>
      if (first) {
        projectName =
          Option(section.title.content.collect { case container: TextContainer => container.content }.mkString)
        println(s"[First Section Title]${section.title}")
        first = false
      }
    }
    Right(
      Changelog(
        projectName = projectName,
        description = description,
        entries = IndexedSeq()
      )
    )
  }

  def fromMarkdownText(markdownText: String): Either[ParsingError, Changelog] = {
    val parserBuilder = MarkupParser.of(Markdown)
    val parser        = parserBuilder.build

    val result = parser.parse(markdownText).left.map(e => ParsingError.ChangelogDocumentParseError(e.message))
    result.flatMap(fromDocument)
  }
}
