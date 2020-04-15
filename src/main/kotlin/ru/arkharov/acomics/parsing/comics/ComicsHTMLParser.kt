package ru.arkharov.acomics.parsing.comics

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.text.ParseException

private const val CONTENT = "content"
private const val SERIAL = "serial"
private const val ISSUE_NAME = "issueName"
private const val ISSUE_NUMBER = "issueNumber"
private const val ISSUE = "issue"
private const val ISSUE_NEXT = "next"
private const val ISSUE_PREV = "prev"
private const val ISSUE_IMAGE = "mainImage"
private const val BASE_ACOMICS_URL = "https://acomics.ru"

@Component
class ComicsHTMLParser {
	
	@Throws(ParseException::class)
	fun parse(html: String) : ComicsPageData {
		val doc: Document = Jsoup.parse(html)
		val issueContainer = doc.getElementById(CONTENT)
		
		val serial = issueContainer.getElementsByClass(SERIAL).first()
		val issueName = serial.getElementsByClass(ISSUE_NAME)
			.first()
			.text()
		
		val issueNumber = serial.getElementsByClass(ISSUE_NUMBER)
			.first()
			.text()
		
		val issue = issueContainer.getElementsByClass(ISSUE).first()
		val nextPageLink = issue.getElementsByClass(ISSUE_NEXT)
			.first()
			?.attr("href")
		val prevPageLink = issue.getElementsByClass(ISSUE_PREV)
			.first()
			?.attr("href")
		val imagePostfix = issue.getElementById(ISSUE_IMAGE)
			.attr("src")
		val imageLink = BASE_ACOMICS_URL + imagePostfix
		
		return ComicsPageData(
			imageLink,
			issueName,
			issueNumber,
			nextPageLink,
			prevPageLink
		)
	}
}