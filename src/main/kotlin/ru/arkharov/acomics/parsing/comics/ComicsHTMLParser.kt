package ru.arkharov.acomics.parsing.comics

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Whitelist
import org.springframework.stereotype.Component
import java.text.ParseException

private const val BASE_ACOMICS_URL = "https://acomics.ru"
private const val CONTENT = "content"
private const val SERIAL = "serial"

private const val ISSUE_NAME = "issueName"
private const val ISSUE_NUMBER = "issueNumber"
private const val ISSUE = "issue"
private const val ISSUE_NEXT = "next"
private const val ISSUE_PREV = "prev"
private const val ISSUE_IMAGE = "mainImage"

private const val AUTHOR_BLOCK_AUTHORS = "authors"
private const val AUTHOR_BLOCK_AVATAR_TAG = "img"
private const val AUTHOR_BLOCK_AVATAR = "avatar"
private const val AUTHOR_BLOCK_INFO = "info"
private const val AUTHOR_BLOCK_USER_NAME = "userName"
private const val AUTHOR_BLOCK_TITLE = "title"
private const val AUTHOR_BLOCK_TIME = "time"
private const val COMMENTS_BLOCK_TIME_ATTR = "data-value"
private const val AUTHOR_BLOCK_COMMENT = "description"

private const val COMMENTS_BLOCK_ID = "contentMargin"
private const val COMMENT_BLOCK = "inner"
private const val COMMENT_BLOCK_INFO = "info"
private const val COMMENT_BLOCK_USERMANE = "username"
private const val COMMENT_BLOCK_ROLE = "role"
private const val COMMENT_BLOCK_COMMENT_ID = "id"
private const val COMMENT_BLOCK_COMMENT_EDITED = "edited"

@Component
class ComicsHTMLParser {
	
	@Throws(ParseException::class)
	fun parse(html: String): ParsedComicsPage {
		val doc: Document = Jsoup.parse(html)
		val contentContainer: Element = doc.getElementById(CONTENT)
		
		val serial = contentContainer.getElementsByClass(SERIAL).first()
		val issueName = serial.getElementsByClass(ISSUE_NAME)
			.first()
			.text()
		
		val issueNumber = serial.getElementsByClass(ISSUE_NUMBER)
			.first()
			.text()
		
		val issue = contentContainer.getElementsByClass(ISSUE).first()
		val nextPageLink = issue.getElementsByClass(ISSUE_NEXT)
			.first()
			?.attr("href")
		val prevPageLink = issue.getElementsByClass(ISSUE_PREV)
			.first()
			?.attr("href")
		val imagePostfix = issue.getElementById(ISSUE_IMAGE)
			.attr("src")
		val imageLink = "$BASE_ACOMICS_URL$imagePostfix"
		
		val authors: Element = contentContainer.getElementsByClass(AUTHOR_BLOCK_AUTHORS).first()
		val avatarUrlPostfix: String = authors.getElementsByClass(AUTHOR_BLOCK_AVATAR)
			.first()
			.getElementsByTag(AUTHOR_BLOCK_AVATAR_TAG)
			.attr("src")
		
		val infoElement: Element = authors.getElementsByClass(AUTHOR_BLOCK_INFO).first()
		val userName: String = infoElement.getElementsByClass(AUTHOR_BLOCK_USER_NAME)
			.first()
			.text()
		val issueTitle: String = infoElement.getElementsByClass(AUTHOR_BLOCK_TITLE)
			?.first()
			?.text()
			?: ""
		
		// Возвращает bkup формата "=77786159", что является отступом в секундах от текущего unix-времени.
		val issueBkupDate: String = infoElement.getElementsByClass(AUTHOR_BLOCK_TIME)
			.first()
			.text()
			.replace("=", "")
		val issueDate: Long? = try {
			val bkupDate = issueBkupDate.toLongOrNull()
			val currentDateInSeconds = System.currentTimeMillis() / 1000
			if (bkupDate == null) {
				null
			} else currentDateInSeconds - bkupDate
		} catch (ignored: Exception) {
			null
		}
		
		val authorsCommentBodyHtml = authors.getElementsByClass(AUTHOR_BLOCK_COMMENT)
		val authorsCommentBody = Jsoup.clean(
			authorsCommentBodyHtml.html(),
			doc.baseUri(),
			Whitelist().addAttributes("a", "href").addAttributes("img", "src"),
			Document.OutputSettings().prettyPrint(true).outline(true)
		).replace("\n", "\n\n")
		
		val uploaderComment = ParsedUploaderComment(
			userName,
			"$BASE_ACOMICS_URL$avatarUrlPostfix",
			issueDate,
			issueTitle,
			authorsCommentBody
		)
		val comments: List<ParsedComicsComment> = parseComments(contentContainer)
		return ParsedComicsPage(
			imageUrl = imageLink,
			issueName = issueName,
			issueNumber = issueNumber,
			nextPageAddress = nextPageLink,
			prevPageAddress = prevPageLink,
			parsedUploaderComment = uploaderComment,
			comments = comments
		)
	}
	
	private fun parseComments(contentContainer: Element): List<ParsedComicsComment> {
		try {
			val commentsBlock: Element = contentContainer.getElementById(COMMENTS_BLOCK_ID)
			val rawComments: List<Element> = commentsBlock.children().filter { element: Element ->
				return@filter element.className()?.contains("comment ") ?: false
			}
			return rawComments.map { element: Element ->
				val avatarUrl: String = element.getElementsByClass(AUTHOR_BLOCK_AVATAR)
					.first()
					.getElementsByTag(AUTHOR_BLOCK_AVATAR_TAG)
					.attr("src")
				val rawCommentBlock: Element = element.getElementsByClass(COMMENT_BLOCK).first()
				val commentBody: String = rawCommentBlock.ownText()
				val commentsData: Element = rawCommentBlock.getElementsByClass(COMMENT_BLOCK_INFO).first()
				val userName: String = commentsData.getElementsByClass(COMMENT_BLOCK_USERMANE).first().text()
				val date: String = commentsData.getElementsByClass(AUTHOR_BLOCK_TIME).attr(COMMENTS_BLOCK_TIME_ATTR)
				val userStatus: String? = commentsData.getElementsByClass(COMMENT_BLOCK_ROLE).first()?.text()
				val commentId: String = commentsData.getElementsByClass(COMMENT_BLOCK_COMMENT_ID).first().text()
				val editedDate: String? = rawCommentBlock.getElementsByClass(COMMENT_BLOCK_COMMENT_EDITED).first()?.text()
				return@map ParsedComicsComment(
					userName,
					"$BASE_ACOMICS_URL$avatarUrl",
					userStatus,
					date,
					commentBody,
					commentId,
					editedDate
				)
			}
		} catch (e: Exception) {
			return emptyList()
		}
	}
}