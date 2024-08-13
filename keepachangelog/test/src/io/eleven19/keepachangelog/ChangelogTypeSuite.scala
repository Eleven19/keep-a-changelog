package io.eleven19.keepachangelog

class ChangelogTypeSuite extends munit.FunSuite {
  test("Sorting a list of change types") {
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

    assertEquals(
      sortedChangeTypes,
      List(
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
}
