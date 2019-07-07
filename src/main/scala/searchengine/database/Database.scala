package searchengine.database

import searchengine.processors._
import slick.dbio.Effect
import slick.jdbc.MySQLProfile.api._
import slick.lifted.QueryBase
import slick.sql.FixedSqlAction

import scala.concurrent.Await
import scala.concurrent.duration.Duration

final case class WordOccurrenceCount(word: String, link: String, occurrenceCount: Int)

final class WordOccurrenceCountTable(tag: Tag) extends Table[WordOccurrenceCount](tag, "word-occurrence-count") {
  def link = column[String]("link")

  def word = column[String]("word")

  def occurrenceCount = column[Int]("occurrenceCount")

  def * = (word, link, occurrenceCount).mapTo[WordOccurrenceCount]

  def pk = primaryKey("pk", (link, word))
}

final case class LinkReferencesCount(link: String, references: Int)

final class LinkReferencesCountTable(tag: Tag) extends Table[LinkReferencesCount](tag, "link-references-count") {
  def link = column[String]("link", O.PrimaryKey)

  def referenceCount = column[Int]("referenceCount")

  def * = (link, referenceCount).mapTo[LinkReferencesCount]
}

object Database {
  private val database = slick.jdbc.MySQLProfile.backend.Database.forConfig("search-engine")

  // Base query for querying the word occurrence table:
  private val wordOccurrencesCountQuery = TableQuery[WordOccurrenceCountTable]

  // Base query for querying the link references table:
  private val linkReferencesCountQuery = TableQuery[LinkReferencesCountTable]

  private def createTablesAction: DBIOAction[Unit, NoStream, Effect.Schema] =
    wordOccurrencesCountQuery.schema.create andThen linkReferencesCountQuery.schema.create

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

  def addLinkReferencesToDatabase(references: LinkReferencesMap): Unit = references.linkToReferences.foreach(keyValuePair => {
    val selectLinkReference =
      Await.result(database.run(
        linkReferencesCountQuery.filter(_.link === keyValuePair._1).map(_.referenceCount).result),
        Duration.Inf)

    if (selectLinkReference.isEmpty) {
      // insert
      Await.result(database.run(
        insertAction(LinkReferencesCount(keyValuePair._1, keyValuePair._2))), Duration.Inf)
    }
    else {
      // update
      Await.result(database.run(
        updateAction(LinkReferencesCount(keyValuePair._1, keyValuePair._2 + selectLinkReference.head))), Duration.Inf)
    }
  })

  def addWordOccurenceCountToDatabase(wordOccurrence: WordOccurence): Unit =
    wordOccurrence.linkWordOccurenceMap.foreach { keyValuePair =>
      val (curLink, wordOccurrenceMap) = keyValuePair

      wordOccurrenceMap.foreach { keyValuePair =>
        val (word, occurence) = keyValuePair

        val selectWordOccurence =
          Await.result(database.run(
            wordOccurrencesCountQuery.filter(
              tuple => tuple.link === curLink && tuple.word === word).result), Duration.Inf)

        if (selectWordOccurence.isEmpty) {
          // insert
          Await.result(database.run(insertAction(WordOccurrenceCount(word, curLink, occurence))), Duration.Inf)
        }
        else {
          // update
          Await.result(database.run(
            updateAction(WordOccurrenceCount(word, curLink, selectWordOccurence.head.occurrenceCount + occurence))),
            Duration.Inf)
        }
      }
    }
}
