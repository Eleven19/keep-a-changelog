package io.eleven19.keepachangelog.errors

import scala.annotation.tailrec

sealed trait ErrorCause[+E] extends Product with Serializable { self =>
  import ErrorCause._

  def &&[E1 >: E](that: ErrorCause[E1]): ErrorCause[E1] = Both(self, that)
  def ++[E1 >: E](that: ErrorCause[E1]): ErrorCause[E1] =
    if (self eq Empty) that else if (that eq Empty) self else Then(self, that)

  def andThen[E1 >: E](that: ErrorCause[E1]): ErrorCause[E1] = self ++ that

  /// Produces a list of all recoverable errors `E` in the `ErrorCause`.
  final def failures: List[E] =
    self.foldLeft(List.empty[E]) { case (z, Fail(v)) =>
      v :: z
    }
      .reverse

  final def foldLeft[Z](z: Z)(f: PartialFunction[(Z, ErrorCause[E]), Z]): Z = {
    @tailrec
    def loop(z0: Z, reason: ErrorCause[E], stack: List[ErrorCause[E]]): Z = {
      val z = f.applyOrElse[(Z, ErrorCause[E]), Z](z0 -> reason, _._1)
      reason match {
        case Then(left, right)   => loop(z, left, right :: stack)
        case Both(left, right)   => loop(z, left, right :: stack)
        case _ if stack.nonEmpty => loop(z, stack.head, stack.tail)
        case _                   => z
      }
    }
    if (self eq Empty) z
    else loop(z, self, Nil)

  }

  def render: String = {
    def loop(stack: List[ErrorCause[E]], sb: StringBuilder): StringBuilder =
      stack match {
        case Nil                       => sb
        case Empty :: tail             => loop(tail, sb)
        case Fail(error) :: tail       => loop(tail, sb.append(error.toString).append(System.lineSeparator))
        case Both(left, right) :: tail => loop(left :: right :: tail, sb)
        case Then(left, right) :: tail => loop(left :: right :: tail, sb)
      }
    loop(List(self), new StringBuilder).toString
  }

  // def flatMap[E2](f: E => ErrorCause[E2]):ErrorCause[E2] = self match {
  //     case Empty =>
  // }
  // def map[E1](f: E => E1):ErrorCause[E1] = ???
}

object ErrorCause {
  val empty: ErrorCause[Nothing]                                        = Empty
  def fail[E](error: E): ErrorCause[E]                                  = Fail(error)
  def both[E](left: ErrorCause[E], right: ErrorCause[E]): ErrorCause[E] = Both(left, right)

  case object Empty                                                    extends ErrorCause[Nothing]
  final case class Fail[+E](error: E)                                  extends ErrorCause[E]
  final case class Both[+E](left: ErrorCause[E], right: ErrorCause[E]) extends ErrorCause[E]
  final case class Then[+E](left: ErrorCause[E], right: ErrorCause[E]) extends ErrorCause[E]
}
