package ru.arkharov.acomics.db

import ru.arkharov.acomics.db.CATALOG_TABLE
import javax.persistence.*

//todo поиграться с составными индексами, посмотреть, как ЕП мобилки отрабатывать будет
@Entity
@Table(
	name = CATALOG_TABLE,
	indexes = [
		Index(columnList = "lastUpdated"),
		Index(columnList = "totalPages"),
		Index(columnList = "rating"),
		Index(columnList = "totalSubscribers")
	]
)
data class CatalogEntity(
	val hyperLink: String,
	val previewImage: String,
	@field:Id
	@field:Column(unique = true, nullable = false)
	val title: String,
	val description: String,
	val rating: String,
	val lastUpdated: Long, // Эта штука приходит в формате 15241.
	val totalPages: Int,
	val formattedTotalPages: String, // формата "232 выпуска"
	val ongoningRate: Double, // Коэффициент(выпуска в месяц?) странный, приходит в формате 4.778
	val totalSubscribers: Int
)

//todo here
enum class MPAARating(val queryParamValue: String) {
	UNDEFINED("1"),
	G("2"),
	PG("3"),
	PG_13("4"),
	R("5"),
	NC_17("6");
}