package io.eleven19.keepachangelog
import errors.ParsingError
import laika.api._
import laika.ast._
import laika.format.Markdown
import java.time.LocalDate
import just.semver.SemVer
import cats.data.Op

sealed abstract class ChangelogElement extends Product with Serializable

final case class Changelog(
    projectName: Option[String],
    description: Option[String],
    entries: IndexedSeq[ChangelogEntry]
) extends ChangelogElement {
  self =>
  def +(entry: ChangelogEntry): Changelog                         = self.copy(entries = entries.appended(entry))
  def projectNameOrDefault(default: String): String               = projectName.getOrElse(default)
  def descriptionOrDefault(default: String): String               = description.getOrElse(default)
  def withEntries(entries: IndexedSeq[ChangelogEntry]): Changelog = self.copy(entries = entries)
  def withEntries(entries: ChangelogEntry*): Changelog            = self.copy(entries = entries.toIndexedSeq)
  def withSortedEntries: Changelog                                = self.copy(entries = entries.sorted)
}

object Changelog {
  val empty: Changelog = Changelog(None, None, IndexedSeq())

  def apply(projectName: String): Changelog         = Changelog(Some(projectName), None, IndexedSeq())
  def apply(projectName: Option[String]): Changelog = Changelog(projectName, None, IndexedSeq())
  def apply(projectName: String, description: String): Changelog =
    Changelog(Some(projectName), Some(description), IndexedSeq())

  object HeaderText {
    def unapply(input: Any): Option[String] = input match {
      case element: ElementTraversal =>
        val spans = extractTextSpans(element)
        val text  = spans.map(_.content).mkString
        Option(text)
      case _ => None
    }
  }

  private def extractTextSpans(element: ElementTraversal): List[Text] =
    element.collect {
      case (h1 @ Header(1, _, _)) => h1.collect { case span: Text => span }
    }.flatten

  private[keepachangelog] def fromSection(section: Section) = {
    val projectName = section.header.extractText
    val (paragraphs, _) = {
      val paras = List.newBuilder[Paragraph]
      val secs  = List.newBuilder[Section]
      section.content.foreach {
        case p: Paragraph => paras += p
        case s: Section   => secs += s
        case _            =>
      }
      (paras.result(), secs.result())
    }
    val description = paragraphs.map(_.extractText).mkString(System.lineSeparator())
    Changelog(
      projectName = projectName,
      description = description
    )
  }

  def fromDocument(document: Document): Changelog =
    document.content.collect {
      case (section @ Section(Header(1, _, _), _, _)) => Changelog.fromSection(section)
    }.take(1).head

  def fromMarkdownText(markdownText: String): Either[ParsingError, Changelog] = {
    val parserBuilder = MarkupParser.of(Markdown)
    val parser        = parserBuilder.build

    val result = parser.parse(markdownText).left.map(e => ParsingError.ChangelogDocumentParseError(e.message))
    result.map(fromDocument)
  }
}

sealed trait ChangelogEntry extends ChangelogElement with Ordered[ChangelogEntry] { self =>

  def compare(that: ChangelogEntry): Int =
    (self, that) match {
      case (ChangelogEntry.Unreleased(_), ChangelogEntry.Unreleased(_)) =>
        0
      case _ => ???
    }

  def wasYanked: Boolean = self match {
    case ChangelogEntry.Released(_, _, yanked) => yanked
    case _                                     => false
  }

  def releaseDate: Option[LocalDate] = self match {
    case ChangelogEntry.Released(_, date, _) => Some(date)
    case _                                   => None
  }
}

sealed trait VersionedChangelogEntry extends ChangelogEntry {
  def version: SemVer
}

object ChangelogEntry {

  object Version {
    def unapply(input: String): Option[SemVer] = SemVer.parse(input).toOption
  }

  final case class Released(version: SemVer, date: LocalDate, yanked: Boolean) extends VersionedChangelogEntry
  final case class Versioned(version: SemVer)                                  extends VersionedChangelogEntry
  final case class Unreleased(contents: IndexedSeq[ChangelogSection])          extends ChangelogEntry

}

final case class ChangelogSection(changeType: ChangeType, items: IndexedSeq[String]) extends ChangelogElement { self =>
  def isCustomChangeType: Boolean = changeType match {
    case ChangeType.Custom(_) => true
    case _                    => false
  }
}

object ChangelogSection {
  implicit val ordering: Ordering[ChangelogSection] = Ordering.by((section: ChangelogSection) => section.changeType)
}
