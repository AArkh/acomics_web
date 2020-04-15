package ru.arkharov.acomics.db

import javax.persistence.*

@Entity
@Table(
	name = COMICS_TABLE,
	indexes = [Index(columnList = "comicsTitle", unique = false)]
)
data class ComicsPageEntity(
	val comicsTitle: String,
	@field:Id
	val imageUrl: String,
	val issueName: String, // Название главы или странички, может отсутствовать.
	@field:ManyToOne(fetch = FetchType.LAZY)
	val catalog: CatalogEntity
)