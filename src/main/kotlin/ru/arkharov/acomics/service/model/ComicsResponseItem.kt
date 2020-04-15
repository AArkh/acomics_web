package ru.arkharov.acomics.service.model

data class ComicsResponseItem(
	val comicsTitle: String,
	val imageUrl: String,
	val issueName: String // Название главы или странички, может отсутствовать.
)