package ru.arkharov.acomics.parsing.comics

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
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
private const val AUTHOR_BLOCK_TIME = "time"
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
	fun parse(html: String): ComicsPageData {
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
		val issueDate: String = infoElement.getElementsByClass(AUTHOR_BLOCK_TIME)
			.first()
			.text()
		val authorsCommentBody: String = authors.getElementsByClass(AUTHOR_BLOCK_COMMENT).text()
		
		val uploaderComment = UploaderComment(
			userName,
			"$BASE_ACOMICS_URL$avatarUrlPostfix",
			issueDate,
			authorsCommentBody
		)
		val comments: List<ComicsComments> = parseComments(contentContainer)
		return ComicsPageData(
			imageLink,
			issueName,
			issueNumber,
			nextPageLink,
			prevPageLink,
			uploaderComment,
			comments
		)
	}
	
	private fun parseComments(contentContainer: Element): List<ComicsComments> {
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
				val formattedDate: String = commentsData.getElementsByClass(AUTHOR_BLOCK_TIME).first().text()
				val userStatus: String? = commentsData.getElementsByClass(COMMENT_BLOCK_ROLE).first()?.text()
				val commentId: String = commentsData.getElementsByClass(COMMENT_BLOCK_COMMENT_ID).first().text()
				val editedDate: String? = rawCommentBlock.getElementsByClass(COMMENT_BLOCK_COMMENT_EDITED).first()?.text()
				return@map ComicsComments(
					userName,
					"$BASE_ACOMICS_URL$avatarUrl",
					userStatus,
					formattedDate,
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