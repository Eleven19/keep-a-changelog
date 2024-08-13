package io.eleven19.keepachangelog

import java.time.LocalDate
import just.semver.SemVer

sealed abstract class ChangelogEntry extends Product with Serializable with Ordered[ChangelogEntry] { self =>

  def compare(that: ChangelogEntry): Int =
    (self, that) match {
      case (ChangelogEntry.Unreleased(_), ChangelogEntry.Unreleased(_)) =>
        0
      case _ => ???
    }

  def wasYanked: Boolean = self match {
    case ChangelogEntry.Released(_, _, yanked) => yanked
    case ChangelogEntry.Unreleased(_)          => false
  }

  def releaseDate: Option[LocalDate] = self match {
    case ChangelogEntry.Released(_, date, _) => Some(date)
    case ChangelogEntry.Unreleased(_)        => None
  }
}

object ChangelogEntry {
  final case class Released(version: SemVer, date: LocalDate, yanked: Boolean) extends ChangelogEntry
  final case class Unreleased(index: Int)                                      extends ChangelogEntry

}

