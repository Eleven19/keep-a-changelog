package io.eleven19.keepachangelog.errors

sealed trait Error        extends Throwable with Product with Serializable
sealed trait ParsingError extends Error
object ParsingError {}
