package ru.arkharov.acomics.service.model

data class SearchResponseCatalogItem(
	val catalog_id: String,
	val title: String,
	val preview_image: String,
	val description: String,
	val rating: String,
	val last_updated: Long ,
	val total_pages: Int,
	val total_subscribers: Int
)