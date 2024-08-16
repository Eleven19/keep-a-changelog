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

abstract class SubtypeBase[A] { self =>
  type Base
  trait __Tag extends Any
  type Type <: A with __Tag

  @inline def unwrap(x: Type): A = x.asInstanceOf[A]
}

abstract class Subtype[A] extends SubtypeBase[A] {
  @inline final def apply(a: A): Type    = a.asInstanceOf[Type]
  @inline protected def wrap(a: A): Type = a.asInstanceOf[Type]

  implicit final class Ops(val self: Type) {
    @inline final def value: A  = Subtype.this.unwrap(self)
    @inline final def unwrap: A = Subtype.this.unwrap(self)
  }

  def unapply(instance: Type): Option[A] = Some(instance.value)
}
