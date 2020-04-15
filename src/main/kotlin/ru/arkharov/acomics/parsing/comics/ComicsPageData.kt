package ru.arkharov.acomics.parsing.comics

data class ComicsPageData(
	val imageUrl: String,
	val issueName: String, // Название главы или странички, может отсутствовать.
	val issueNumber: String, //формата "12/13"
	val nextPageAddress: String?, // Адрес следующей странички. null, ежели это последняя страница
	val prevPageAddress: String? // Адрес предыдущие странички. null, ежели это первая страница
)