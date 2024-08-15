package io.eleven19

import io.eleven19.keepachangelog.errors._
import io.eleven19.keepachangelog.internal.Subtype
import just.semver.SemVer
import cats.syntax.all._
import cats.Show

package object keepachangelog {
  type Version = Version.Type
  object Version extends Subtype[SemVer] {
    def parse(s: String): Either[ParsingError, Version] =
      SemVer.parse(s).map(wrap(_)).leftMap(ParsingError.SemVerParseError(_))
    implicit val ShowVersion: Show[Version]         = Show.show(v => v.value.toString)
    implicit val VersionOrdering: Ordering[Version] = Ordering.by(_.value)

    extension (instance: Version)
      def compare(other: Version): Int = VersionOrdering.compare(instance, other)
      def render: String               = instance.value.render

  }
}
