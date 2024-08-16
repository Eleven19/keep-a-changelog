package io.eleven19.keepachangelog

import laika.ast._

object Changelogs {
  private[keepachangelog] def fromDocument(document: Document) =
    document.content.collect {
      case (section @ Section(Header(1, _, _), _, _)) => Changelog.fromSection(section)
    }

}
