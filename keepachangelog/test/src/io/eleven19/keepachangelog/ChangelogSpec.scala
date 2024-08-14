package io.eleven19.keepachangelog
import zio._
import zio.test._
import laika.api._
import laika.ast._
import laika.format._

object ChangelogSpec extends ZIOSpecDefault {

  def spec: Spec[Any, Throwable] =
    suite("ChangelogSpec")(
      suite("A Changelog")(
        suite("created from markdown text") {
          for {
            changelogContents <-
              Live.live(
                ChangelogSampleDataProvider.standardSampleContent
              ) // NOTE: In order to get the real environment variables we need to use Live.live
            changelog <- ZIO.fromEither(Changelog.fromMarkdownText(changelogContents))            
            _         <- ZIO.logInfo(s"[Changelog]: $changelog")            
          } yield Chunk(
            test("should have the correct projectName") {
              assertTrue(changelog.projectName == Some("Changelog"))
            },
            test("should have the correct description") {
              assertTrue(
                changelog.description.get.startsWith(
                  "All notable changes to this project will be documented in this file."
                )
              )
            },
            suite("Exploring Markdown parsing")(
              test("Document transforms") {
                val parserBuilder = MarkupParser.of(Markdown)
                val parser        = parserBuilder.build
                val result        = parser.parse(changelogContents)

                result.fold(
                  e => println(s"Failed to parse markdown: $e"),
                  document => transformDocument(document)
                )
                assertTrue(result.isRight)
              }
            )
          )
        }
      )
    ).provideShared(ChangelogSampleDataProvider.live)

  def transformDocument(document: Document) = {
    val stack     = scala.collection.mutable.Stack.empty[ChangelogElement]
    var changelog = Changelog.empty
    val headers = document.content.collect {
      case (h1 @ Header(1, _, _)) =>
        val projectName = Option(h1.extractText)
        val description = Option(h1.collect {
          case container: TextContainer => container.content
        }.mkString)
        changelog = changelog.copy(projectName = projectName, description = description)
        stack.push(changelog)
        h1
      case (h2 @ Header(2, _, _)) =>
        val textNodes = h2.collect {
          case container: TextContainer => container.content
        }
        val maybeEntry = textNodes match {
          case "Unreleased" :: Nil                 => Some(ChangelogEntry.Unreleased(IndexedSeq.empty))
          case ChangelogEntry.Version(semVer) :: _ => Some(ChangelogEntry.Versioned(semVer))
          case _                                   => None
        }
        maybeEntry.foreach { entry =>
          stack.push(entry)
          changelog = changelog + entry
        }
        h2
    }

    val sections = document.content.collect {
      case (section @ Section(header @ Header(1, _, _), contents, _)) =>
        val items =
          for {
            content <- contents.map {
              case Section(h, _, _)            => s"Section: ${h.extractText}"
              case (p @ Paragraph(content, _)) => s"P: ${p.extractText}"
              case _                           => "Something else"
            }
          } yield content
        (header.level, header.extractText, items)
    }
    // println(s"headers: $headers")
    println("=================================================")
    println(s"changelog: $changelog")
    println("=================================================")
    println(s"sections: $sections")
  }
}
