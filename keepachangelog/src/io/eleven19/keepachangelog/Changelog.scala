package io.eleven19.keepachangelog
import errors.ParsingError
import laika.api._
import laika.ast._
import laika.format.Markdown
import java.time.LocalDate
import scala.util.matching.Regex
import io.eleven19.keepachangelog.Version

sealed abstract class ChangelogElement extends Product with Serializable

final case class Changelog(
    projectName: Option[String],
    description: Option[String],
    entries: IndexedSeq[ChangelogEntry]
) extends ChangelogElement { self =>
  import Changelog._
  import ChangelogEntry._

  def +(entry: ChangelogEntry): Changelog                         = self.copy(entries = entries.appended(entry))
  def projectNameOrDefault(default: String): String               = projectName.getOrElse(default)
  def descriptionOrDefault(default: String): String               = description.getOrElse(default)
  def withEntries(entries: IndexedSeq[ChangelogEntry]): Changelog = self.copy(entries = entries)
  def withEntries(entries: ChangelogEntry*): Changelog            = self.copy(entries = entries.toIndexedSeq)
  def withSortedEntries: Changelog                                = self.copy(entries = entries.sorted)
  def allUnreleased: IndexedSeq[Unreleased]                       = entries.collect { case u: Unreleased => u }
  def unreleased: Option[Unreleased]                              = entries.collectFirst { case u: Unreleased => u }
  def pending: IndexedSeq[PendingChangelogEntry] = entries.collect { case p: PendingChangelogEntry => p }
  def lastOfKind: LastOfKind = {
    var result = LastOfKind(None, None, None)
    entries.foreach { entry =>
      (entry, result) match {
        case (candidate @ Unreleased(_), _) =>
          // The last encountered unreleased entry is always selected
          result = result.copy(unreleased = Some(candidate))
        case (candidate @ Versioned(_, _), LastOfKind(_, None, _)) =>
          // If we haven't seen a versioned entry yet, select this one
          result = result.copy(versioned = Some(candidate))
        case (candidate @ Versioned(_, _), LastOfKind(_, Some(Versioned(currentVersion, _)), _)) =>
          // If we have seen a versioned entry, select this one if it has a higher version
          if (candidate.version > currentVersion) result = result.copy(versioned = Some(candidate))
        case (candidate @ Released(_, _, _, _), LastOfKind(_, _, None)) =>
          // If we haven't seen a released entry yet, select this one
          result = result.copy(released = Some(candidate))
        case (candidate @ Released(_, _, _, _), LastOfKind(_, _, Some(Released(currentVersion, _, _, _)))) =>
          // If we have seen a released entry, select this one if it has a higher version
          if (candidate.version > currentVersion) result = result.copy(released = Some(candidate))
      }
    }
    result
  }

  def lastVersionOfKind: LastVersionOfKind = lastOfKind.lastVersionOfKind
}

object Changelog {
  import ChangelogEntry._
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
    val (paragraphs, sections) = {
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
    val entries     = sections.map(ChangelogEntry.fromSection)
    Changelog(
      projectName = Option(projectName),
      description = Option(description),
      entries = entries.toIndexedSeq
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

  final case class LastOfKind(
      unreleased: Option[Unreleased],
      versioned: Option[Versioned],
      released: Option[Released]
  ) { self =>
    def lastVersionOfKind: LastVersionOfKind =
      LastVersionOfKind(
        unreleased = self.unreleased.map(_ => ()),
        versioned = self.versioned.map(_.version),
        released = self.released.map(_.version)
      )

  }
  final case class LastVersionOfKind(unreleased: Option[Unit], versioned: Option[Version], released: Option[Version]) {
    self =>
    def hasUnreleased: Boolean = self.unreleased.isDefined
    def hasVersioned: Boolean  = self.versioned.isDefined
    def hasReleased: Boolean   = self.released.isDefined
  }
}

sealed trait ChangelogEntry extends ChangelogElement { self =>
  import ChangelogEntry._

  final def hasReleaseDate: Boolean = self match {
    case Released(_, _, _, _) => true
    case _                    => false
  }

  final def releaseDate: Option[LocalDate] = self match {
    case Released(_, date, _, _) => Some(date)
    case _                       => None
  }

  def sections: IndexedSeq[ChangelogSection]

  final def tag: Tag = self match {
    case _: Unreleased => Tag.Unreleased
    case _: Versioned  => Tag.Versioned
    case _: Released   => Tag.Released
  }

  final def versionOption: Option[Version] = self match {
    case Versioned(version, _)      => Some(version)
    case Released(version, _, _, _) => Some(version)
    case _                          => None
  }

  final def wasYanked: Boolean = self match {
    case Released(_, _, yanked, _) => yanked
    case _                         => false
  }

  final def withSections(sections: IndexedSeq[ChangelogSection]): ChangelogEntry = self match {
    case (entry @ Versioned(_, _))      => entry.copy(sections = sections)
    case (entry @ Released(_, _, _, _)) => entry.copy(sections = sections)
    case (entry @ Unreleased(_))        => entry.copy(sections = sections)
  }
}

object ChangelogEntry {

  implicit val ChangelogEntryOrdering: Ordering[ChangelogEntry] =
    Ordering.by[ChangelogEntry, Int](_.tag.value).orElseBy(_.releaseDate).orElseBy(_.wasYanked)

  private[keepachangelog] def fromSection(section: Section): ChangelogEntry = {
    val textSpans = section.header.collect { case Text(content, _) => content }
    val sections  = section.content.collect { case s: Section => ChangelogSection.fromSection(s) }.toIndexedSeq
    textSpans match {
      case Extractors.Unreleased() => Unreleased(sections = sections)
      case Extractors.HasVersionAndReleaseDate(version, releaseDate, yanked) =>
        Released(version, releaseDate, yanked = yanked, sections = sections)
      case Extractors.HasVersion(version) :: _ => Versioned(version, sections = sections)
      // case Released(version, date, yanked) => Released(version, date, yanked)
      case _ => throw new RuntimeException(s"Could not parse section header: $textSpans")
    }
  }

  sealed trait VersionedChangelogEntry extends ChangelogEntry {
    def version: Version
  }

  sealed trait PendingChangelogEntry extends ChangelogEntry

  object PendingChangelogEntry {
    def unapply(entry: ChangelogEntry): Option[(Tag, Option[Version])] = entry match {
      case v: Versioned  => Some((v.tag, Some(v.version)))
      case v: Unreleased => Some((v.tag, None))
      case _             => None
    }
  }

  final case class Released(
      version: Version,
      date: LocalDate,
      yanked: Boolean,
      sections: IndexedSeq[ChangelogSection] = IndexedSeq.empty
  ) extends VersionedChangelogEntry
  final case class Versioned(version: Version, sections: IndexedSeq[ChangelogSection] = IndexedSeq.empty)
      extends VersionedChangelogEntry with PendingChangelogEntry
  final case class Unreleased(sections: IndexedSeq[ChangelogSection] = IndexedSeq.empty) extends ChangelogEntry
      with PendingChangelogEntry

  sealed abstract class Tag(val value: Int) extends Product with Serializable
  object Tag {
    case object Unreleased extends Tag(0)
    case object Versioned  extends Tag(1)
    case object Released   extends Tag(2)
  }

  object Extractors {
    private val HeaderSeparatorPattern: Regex = "^\\s*-\\s*".r

    object Unreleased {
      def unapply(input: Any): Boolean =
        input match {
          case versionString: String        => versionString.compareToIgnoreCase("Unreleased") == 0
          case (versionString: String) :: _ => versionString.compareToIgnoreCase("Unreleased") == 0
          case _                            => false
        }
    }

    object HasVersion {
      def unapply(input: String): Option[Version] = ??? // Version.parse(input).toOption
      def unapply(input: List[String]): Option[Version] = input match {
        case input :: _ => Version.parse(input).toOption
        case _          => None
      }
      def unapply(entry: ChangelogEntry): Option[Version] = entry match {
        case Released(version, _, _, _) => Some(version)
        case Versioned(version, _)      => Some(version)
        case _                          => None
      }
    }

    object HasVersionAndReleaseDate {
      def unapply(input: List[String]): Option[(Version, LocalDate, Boolean)] = input match {
        case version :: date :: "[YANKED]" :: _ =>
          val releaseDate = LocalDate.parse(date.replaceFirst(HeaderSeparatorPattern.regex, ""))
          Version.parse(version).toOption.map((_, releaseDate, true))
        case version :: date :: _ =>
          val releaseDate = LocalDate.parse(date.replaceFirst(HeaderSeparatorPattern.regex, ""))
          Version.parse(version).toOption.map((_, releaseDate, false))
        case _ => None
      }
    }
  }
}

final case class ChangelogSection(changeType: ChangeType, items: IndexedSeq[String]) extends ChangelogElement { self =>
  def isCustomChangeType: Boolean = changeType match {
    case ChangeType.Custom(_) => true
    case _                    => false
  }
}

object ChangelogSection {
  implicit val ordering: Ordering[ChangelogSection] = Ordering.by((section: ChangelogSection) => section.changeType)

  private[keepachangelog] def fromSection(section: Section): ChangelogSection = {
    val changeType = section.collect { case Text(content, _) => content } match {
      case ChangeType.Parse(changeType, _) => changeType
      case _                               => ChangeType.Custom("unknown")
    }

    val items = section.collect {
      case (bi @ BulletListItem(_, _, _)) =>
        (for {
          text <- bi.collect { case Text(content, _) => content }

        } yield text).mkString(System.lineSeparator())
    }

    ChangelogSection(changeType, items.toIndexedSeq)
  }
}
