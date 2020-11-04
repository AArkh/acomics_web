package ru.arkharov.acomics

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTypeExcludeFilter
import org.springframework.web.reactive.function.client.WebClient
import ru.arkharov.acomics.parsing.comics.ComicsHTMLParser
import ru.arkharov.acomics.parsing.comics.ParsedComicsComment
import ru.arkharov.acomics.parsing.comics.ParsedComicsPage

@DataJdbcTest
@AutoConfigureDataJpa
@TypeExcludeFilters(DataJpaTypeExcludeFilter::class)
@AutoConfigureTestEntityManager
class ComicsParsingTest {
	
	@field:Autowired
	private var comicsHTMLParser: ComicsHTMLParser? = null
	@field:Autowired
	private var webClient: WebClient? = null
	
	private val comicsLinks = arrayOf(
		"https://acomics.ru/~outsider",
		"https://acomics.ru/~WSWfG",
		"https://acomics.ru/~4pairs",
		"https://acomics.ru/~Quested",
		"https://acomics.ru/~ShutEye-1",
		"https://acomics.ru/~BFT12",
		"https://acomics.ru/~tokatokatoka"
	)
	
	@Test
	fun pageParserTest() {
		val parser = comicsHTMLParser ?: throw IllegalStateException()
		val comments = mutableListOf<ParsedComicsComment>()
		val uploaderComments = mutableListOf<String>()
		comicsLinks.forEach { comicsLink: String ->
			repeat(10) {
				val webPage: String = retrieveComicsPageHtml("$comicsLink/${it.inc()}")
				val parsedComicsPage: ParsedComicsPage = parser.parse(webPage)
				assert(parsedComicsPage.imageUrl.isNotEmpty())
				assert(parsedComicsPage.parsedUploaderComment.issueDate != null)
				assert(parsedComicsPage.parsedUploaderComment.userName.isNotEmpty())
				assert(parsedComicsPage.parsedUploaderComment.userProfileUrl.isNotEmpty())
				assert(parsedComicsPage.imageUrl.isNotEmpty())
				comments.addAll(parsedComicsPage.comments)
				val uploaderComment: String? = parsedComicsPage.parsedUploaderComment.commentBody
				if (uploaderComment != null) {
					uploaderComments.add(uploaderComment)
				}
				assert(parsedComicsPage.parsedUploaderComment.issueDate!! > 0L)
			}
			assert(comments.any {
				it.editedData != null
			})
			assert(comments.any {
				it.userStatus != null
			})
			assertDoesNotThrow {
				comments.forEach { assert(it.date.toLong() > 0L) }
			}
			assert(uploaderComments.isNotEmpty())
		}
	}
	
	@Throws(IllegalStateException::class)
	private fun retrieveComicsPageHtml(comicsPageUrl: String): String {
		val resp = webClient!!
			.get()
			.uri(comicsPageUrl)
			.retrieve()
		return resp.bodyToMono(String::class.java).block()
			?: throw IllegalStateException("no body!")
	}
}