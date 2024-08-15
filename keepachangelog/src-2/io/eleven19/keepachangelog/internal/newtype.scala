package io.eleven19.keepachangelog.internal

abstract class NewtypeBase[A] { self =>
  type Base
  trait __Tag extends Any
  type Type <: Base with __Tag

  @inline def unwrap(x: Type): A = x.asInstanceOf[A]
}

abstract class Newtype[A] extends NewtypeBase[A] { self =>
  @inline final def apply(a: A): Type    = a.asInstanceOf[Type]
  @inline protected def wrap(a: A): Type = a.asInstanceOf[Type]

  implicit final class Ops(val self: Type) {
    @inline final def value: A  = Newtype.this.unwrap(self)
    @inline final def unwrap: A = Newtype.this.unwrap(self)
  }

  def unapply(instance: Type): Option[A] = Some(instance.value)
}

abstract class Subtype[A] extends Newtype[A] {
  override type Type <: A
}
