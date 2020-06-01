package ru.arkharov.acomics.service.model

data class ComicsResponseItem(
	val comics_title: String,
	val image_url: String,
	val issue_name: String // Название главы или странички, может отсутствовать.
)