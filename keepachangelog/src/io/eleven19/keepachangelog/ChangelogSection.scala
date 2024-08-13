package io.eleven19.keepachangelog

final case class ChangelogSection(changeType: ChangeType, items: IndexedSeq[String]) { self =>
  def isCustomChangeType: Boolean = changeType match {
    case ChangeType.Custom(_) => true
    case _                    => false
  }
}

object ChangelogSection {
  implicit val ordering: Ordering[ChangelogSection] = Ordering.by((section: ChangelogSection) => section.changeType)
}
