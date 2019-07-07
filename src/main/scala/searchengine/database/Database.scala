package searchengine.database

import searchengine.processors._
import slick.dbio.Effect
import slick.jdbc.MySQLProfile.api._
import slick.sql.FixedSqlAction

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final case class WordOccurrenceCount(word: String, link: String, occurrenceCount: Int)

final class WordOccurrenceCountTable(tag: Tag) extends Table[WordOccurrenceCount](tag, "word_occurrence_count") {
  def link = column[String]("link")

  def word = column[String]("word")

  def occurrenceCount = column[Int]("occurrenceCount")

  def * = (word, link, occurrenceCount).mapTo[WordOccurrenceCount]

  def pk = primaryKey("pk", (link, word))
}

final case class LinkReferencesCount(link: String, references: Int)

final class LinkReferencesCountTable(tag: Tag) extends Table[LinkReferencesCount](tag, "link_references_count") {
  def link = column[String]("link", O.PrimaryKey)

  def referenceCount = column[Int]("referenceCount")

  def * = (link, referenceCount).mapTo[LinkReferencesCount]
}

object Database {
  val database = slick.jdbc.MySQLProfile.backend.Database.forConfig("search-engine")

  // Base query for querying the word occurrence table:
  private val wordOccurrencesCountQuery = TableQuery[WordOccurrenceCountTable]

  // Base query for querying the link references table:
  private val linkReferencesCountQuery = TableQuery[LinkReferencesCountTable]

  def createTablesActions = List(createWordOccurenceTableAction, createLinkReferencesTableAction)

  def createWordOccurenceTableAction = wordOccurrencesCountQuery.schema.create
  def createLinkReferencesTableAction = linkReferencesCountQuery.schema.create

  private def insertAction(tuple: WordOccurrenceCount): FixedSqlAction[Int, NoStream, Effect.Write] =
    wordOccurrencesCountQuery += tuple

  private def insertAction(tuple: LinkReferencesCount): FixedSqlAction[Int, NoStream, Effect.Write] =
    linkReferencesCountQuery += tuple

  private def updateAction(tuple: WordOccurrenceCount): FixedSqlAction[Int, NoStream, Effect.Write] =
    wordOccurrencesCountQuery
      .filter(w => w.link === tuple.link && w.word === tuple.word)
      .map(_.occurrenceCount)
      .update(tuple.occurrenceCount)

  private def updateAction(tuple: LinkReferencesCount): FixedSqlAction[Int, NoStream, Effect.Write] =
    linkReferencesCountQuery
      .filter(_.link === tuple.link)
      .map(_.referenceCount)
      .update(tuple.references)

  private def addLinkReferencesToDatabase(references: LinkReferencesMap): immutable.Iterable[Future[Int]] =
    references.linkToReferences.map { case (link, refs) =>
      database.run(linkReferencesCountQuery.filter(_.link === link).map(_.referenceCount).result).flatMap { res =>
        if (res.isEmpty) database.run(insertAction(LinkReferencesCount(link, refs)))
        else database.run(updateAction(LinkReferencesCount(link, res.head + refs)))
      }
    }

  private def addWordOccurenceCountToDatabase(wordOccurrence: WordOccurrence): immutable.Iterable[Future[Int]] =
    wordOccurrence.linkWordOccurrenceMap.map { case (linkWordPair, count) =>
      database.run(wordOccurrencesCountQuery
        .filter(entry => entry.link === linkWordPair._1 && entry.word === linkWordPair._2).map(_.occurrenceCount).result)
        .flatMap(res =>
          if (res.isEmpty) database.run(insertAction(WordOccurrenceCount(linkWordPair._2, linkWordPair._1, count)))
          else database.run(updateAction(WordOccurrenceCount(linkWordPair._2, linkWordPair._1, count + res.head))))
    }

  def addLinkDataToDatabase(linkData: LinkData) = {
    val f1 = addLinkReferencesToDatabase(linkData.linkReferences)
    val f2 = addWordOccurenceCountToDatabase(linkData.wordOccurrence)

    f1 ++ f2
  }
}
