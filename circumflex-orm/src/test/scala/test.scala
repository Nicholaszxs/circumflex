package ru.circumflex.orm

class Category extends Record {
  def relation = Category
  val id = field(Category.id)
  val name = field(Category.name)

  override def toString =
    if (isIdentified) name.toString
    else "UNKNOWN"
}

object Category extends GenericTable {
  def recordClass = classOf[Category]
  val name = stringColumn("name")       // Creates a column
      .notNull                          // Creates NOT NULL constraint
      .unique                           // Creates UNIQUE constraint
}

class Book extends Record {
  def relation = Book
  val id = field(Book.id)
  val title = field(Book.title)
  val categoryId = field(Book.category.localColumn)
  val category = manyToOne(Book.category)

  override def toString =
    if (isIdentified) title.toString
    else "UNKNOWN"
}

object Book extends GenericTable {
  def recordClass = classOf[Book]
  val title = stringColumn("title")
      .notNull
  val category = longColumn("category_id")
      .references(Category)     // Creates an association with Category
      .onDeleteSetNull
      .onUpdateCascade
}