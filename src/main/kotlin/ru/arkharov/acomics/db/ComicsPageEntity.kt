package ru.arkharov.acomics.db

import ru.arkharov.acomics.db.COMICS_TABLE
import ru.arkharov.acomics.db.CatalogEntity
import javax.persistence.*

@Entity
@Table(
	name = COMICS_TABLE,
	indexes = [Index(columnList = "comicsTitle", unique = false)]
)
open class ComicsPageEntity(
	val comicsTitle: String,
	@field:Id
	val imageUrl: String,
	val issueName: String, // Название главы или странички, может отсутствовать.
	@field:ManyToOne(fetch = FetchType.LAZY)
	val catalog: CatalogEntity
)