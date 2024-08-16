package io.eleven19.keepachangelog.internal

abstract class NewtypeBase[A] { self =>
  type Type
}

abstract class Newtype[A] extends NewtypeBase[A] { self =>
  opaque type T = A
  type Type     = T

  protected def wrap(a: A): Newtype.this.Type = a
  def unapply(instance: Type): Option[A]      = Some(instance.value)
  extension (instance: Type)
    def value: A  = instance
    def unwrap: A = instance

}

abstract class Subtype[A] extends NewtypeBase[A] { self =>
  opaque type T <: A = A
  type Type          = T

  protected def wrap(a: A): Subtype.this.Type = a
  def unapply(instance: Type): Option[A]      = Some(instance.value)
  extension (instance: Type)
    def value: A  = instance
    def unwrap: A = instance

}
