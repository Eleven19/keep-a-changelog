package io.eleven19.keepachangelog

import zio.test._

object ChangeTypeSpec extends ZIOSpecDefault {

  def spec: Spec[Any, Nothing] = suite("ChangeTypeSpec")(
    suite("A ChangelogType List")(
      suite("when sorted")(
        test("should be sorted by the order of the ChangeType enumeration") {
          val changeTypes = List(
            ChangeType.Fixed,
            ChangeType.Added,
            ChangeType.Security,
            ChangeType.Custom("foo"),
            ChangeType.Removed,
            ChangeType.Changed,
            ChangeType.Custom("bar"),
            ChangeType.Deprecated
          )

          val sortedChangeTypes = changeTypes.sorted

          assertTrue(
            sortedChangeTypes == List(
              ChangeType.Added,
              ChangeType.Changed,
              ChangeType.Deprecated,
              ChangeType.Removed,
              ChangeType.Fixed,
              ChangeType.Security,
              ChangeType.Custom("bar"),
              ChangeType.Custom("foo")
            )
          )
        }
      )
    )
  )

}
