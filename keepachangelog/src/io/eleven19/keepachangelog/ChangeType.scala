package io.eleven19.keepachangelog

import scala.annotation.nowarn

sealed abstract class ChangeType extends Product with Serializable with Ordered[ChangeType] { self =>
  def compare(that: ChangeType): Int = (self, that) match {
    case (ChangeType.Custom(left), ChangeType.Custom(right)) => left.compareTo(right)
    case _                                                   => self.order.compareTo(that.order)
  }

  def order: Int = self match {
    case ChangeType.Added      => 0
    case ChangeType.Changed    => 1
    case ChangeType.Deprecated => 2
    case ChangeType.Removed    => 3
    case ChangeType.Fixed      => 4
    case ChangeType.Security   => 5
    case ChangeType.Custom(_)  => 6
  }

}

object ChangeType {
  def fromString(s: String): ChangeType = s.toLowerCase() match {
    case "added"      => Added
    case "changed"    => Changed
    case "deprecated" => Deprecated
    case "removed"    => Removed
    case "fixed"      => Fixed
    case "security"   => Security
    case other        => Custom(other)
  }

  case object Added      extends ChangeType
  case object Changed    extends ChangeType
  case object Deprecated extends ChangeType
  case object Removed    extends ChangeType
  case object Fixed      extends ChangeType
  case object Security   extends ChangeType
  @nowarn
  case class Custom private[keepachangelog] (name: String) extends ChangeType

  object Parse {
    def unapply(s: String): Option[ChangeType] = Some(fromString(s))

    def unapply(segments: List[String]): Option[(ChangeType, List[String])] = segments match {
      case head :: tail => Some((ChangeType.fromString(head), tail))
      case _            => None
    }

  }
}
