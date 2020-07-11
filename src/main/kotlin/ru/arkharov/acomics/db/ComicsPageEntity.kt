package ru.arkharov.acomics.db

import java.io.Serializable
import javax.persistence.*

@Entity
@Table(
	name = COMICS_TABLE,
	indexes = [Index(columnList = "comicsTitle", unique = false)]
)
data class ComicsPageEntity(
	@field:EmbeddedId
	val comicsId: ComicsId,
	val comicsTitle: String,
	val imageUrl: String,
	val issueName: String, // Название главы или странички, может отсутствовать.
	@field:ManyToOne(fetch = FetchType.LAZY)
	@field:MapsId("catalogId")
	val catalog: CatalogEntity
)

@Embeddable
data class ComicsId(
	val catalogId: String,
	val page: Int
) : Serializable