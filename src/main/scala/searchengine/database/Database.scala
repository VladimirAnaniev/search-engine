package searchengine.database

import slick.jdbc.H2Profile.api._

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
  val database = slick.jdbc.H2Profile.backend.Database.forConfig("search-engine")

  // Base query for querying the word occurrence table:
  val wordOccurrencesCountQuery = TableQuery[WordOccurrenceCountTable]

  // Base query for querying the link references table:
  val linkReferencesCountQuery = TableQuery[LinkReferencesCountTable]

  val createTablesAction = wordOccurrencesCountQuery.schema.create andThen linkReferencesCountQuery.schema.create

  def insertAction(tuple: WordOccurrenceCount) = wordOccurrencesCountQuery += tuple

  def insertAction(tuple: LinkReferencesCount) = linkReferencesCountQuery += tuple

  def updateWordOccurenceAction(tuple: WordOccurrenceCount) =
    wordOccurrencesCountQuery
      .filter(w => w.link === tuple.link && w.word === tuple.word)
      .map(_.occurrenceCount)
      .update(tuple.occurrenceCount)


  def updateLinkReferenceAction(tuple: LinkReferencesCount) =
    linkReferencesCountQuery
      .filter(_.link === tuple.link)
      .map(_.referenceCount)
      .update(tuple.references)


}
