package ru.arkharov.acomics.parsing.comics

data class ParsedComicsPage(
	val imageUrl: String,
	val issueName: String, // Название главы или странички, может отсутствовать.
	val issueNumber: String, //формата "12/13"
	val nextPageAddress: String?, // Адрес следующей странички. null, ежели это последняя страница
	val prevPageAddress: String?, // Адрес предыдущие странички. null, ежели это первая страница
	val parsedUploaderComment: ParsedUploaderComment,
	val comments: List<ParsedComicsComment>
)

data class ParsedUploaderComment(
	val userName: String,
	val userProfileUrl: String,
	val issueDate: Long?,
	val commentTitle: String?,
	val commentBody: String?
)

data class ParsedComicsComment(
	val userName: String,
	val userProfileUrl: String,
	val userStatus: String?,
	val date: String,
	val commentBody: String,
	val commentId: String, // формата #1152325
	val editedData: String? // формата "Отредактировано «RusPeanuts» 04.01.2020 02:19:54"
)