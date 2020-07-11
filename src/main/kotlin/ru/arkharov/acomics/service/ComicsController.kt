package ru.arkharov.acomics.service

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import ru.arkharov.acomics.db.COMICS_TABLE
import ru.arkharov.acomics.db.COMICS_UPLOADER_TABLE
import ru.arkharov.acomics.db.COMMENTS_TABLE
import ru.arkharov.acomics.service.model.ComicsResponseItem
import ru.arkharov.acomics.service.model.CommentsResponseItem
import ru.arkharov.acomics.service.model.UploaderData
import java.sql.ResultSet

private const val COMICS_UPLOADER_USER_NAME = "cu_username"
private const val COMICS_UPLOADER_USER_PROFILE = "cu_user_profile"
private const val COMICS_UPLOADER_ISSUE_DATE = "cu_issue_date"
private const val COMICS_UPLOADER_COMMENT_BODY = "cu_comment_body"

private const val COMMENTS_ID = "co_id"
private const val COMMENTS_USER_NAME = "co_user_name"
private const val COMMENTS_USER_STATUS = "co_user_status"
private const val COMMENTS_USER_AVATAR_IMAGE_URL = "co_user_avatar_image_url"
private const val COMMENTS_BODY = "co_body"
private const val COMMENTS_DATE = "co_date"
private const val COMMENTS_EDITED_DATA = "co_edited_data"

@Component
@RestController
class ComicsController(
	private val jdbcTemplate: JdbcTemplate
) {
	
	private val logger = LoggerFactory.getLogger(this::class.java)
	
	@GetMapping("/comics/{catalogId}")
	fun searchComics(@PathVariable(required = true) catalogId: String): List<ComicsResponseItem> {
		logger.info("requested comics with path: $catalogId")
		val startTime = System.currentTimeMillis()
		val rawList: List<RawRowResult> = jdbcTemplate.query("""
			SELECT
				comics_title,
				image_url,
				issue_name,
				page,
				$COMICS_UPLOADER_TABLE.user_name AS $COMICS_UPLOADER_USER_NAME,
				$COMICS_UPLOADER_TABLE.user_profile_url AS $COMICS_UPLOADER_USER_PROFILE,
				$COMICS_UPLOADER_TABLE.issue_date AS $COMICS_UPLOADER_ISSUE_DATE,
				$COMICS_UPLOADER_TABLE.comment_body AS $COMICS_UPLOADER_COMMENT_BODY,
				$COMMENTS_TABLE.comment_id AS $COMMENTS_ID,
				$COMMENTS_TABLE.user_name AS $COMMENTS_USER_NAME,
				$COMMENTS_TABLE.user_avatar_image_url AS $COMMENTS_USER_AVATAR_IMAGE_URL,
				$COMMENTS_TABLE.user_status AS $COMMENTS_USER_STATUS,
				$COMMENTS_TABLE.comment_body AS $COMMENTS_BODY,
				$COMMENTS_TABLE.date AS $COMMENTS_DATE,
				$COMMENTS_TABLE.edited_data AS $COMMENTS_EDITED_DATA
			FROM $COMICS_TABLE
			LEFT JOIN $COMICS_UPLOADER_TABLE
			ON
				$COMICS_TABLE.catalog_catalog_id = $COMICS_UPLOADER_TABLE.comics_page_entity_catalog_catalog_id
				AND $COMICS_TABLE.page = $COMICS_UPLOADER_TABLE.comics_page_entity_page
			LEFT JOIN $COMMENTS_TABLE
			ON
				$COMICS_TABLE.catalog_catalog_id = $COMMENTS_TABLE.comics_catalog_catalog_id
				AND $COMICS_TABLE.page = $COMMENTS_TABLE.comics_page
			WHERE catalog_catalog_id = "$catalogId"
		""".trimIndent()
		) { result: ResultSet, _ ->
			return@query RawRowResult(
				page = result.getInt("page"),
				title = result.getString("comics_title"),
				image = result.getString("image_url"),
				issueName = result.getString("issue_name"),
				issueDate = result.getLong(COMICS_UPLOADER_ISSUE_DATE),
				uploaderName = result.getString(COMICS_UPLOADER_USER_NAME),
				uploaderComment = result.getString(COMICS_UPLOADER_COMMENT_BODY),
				uploaderAvatarUrl = result.getString(COMICS_UPLOADER_USER_PROFILE),
				commentUserName = result.getString(COMMENTS_USER_NAME),
				commentUserAvatarUrl = result.getString(COMMENTS_USER_AVATAR_IMAGE_URL),
				commentUserStatus = result.getString(COMMENTS_USER_STATUS),
				commentBody = result.getString(COMMENTS_BODY),
				commentDate = result.getLong(COMMENTS_DATE),
				commentsId = result.getString(COMMENTS_ID),
				editedText = result.getString(COMMENTS_EDITED_DATA)
			)
		}
		val queryTime = System.currentTimeMillis()
		val resultList = rawList.groupBy { rawRow: RawRowResult ->
			val uploaderData = UploaderData(
				uploader_name = rawRow.uploaderName,
				uploader_avatar_url = rawRow.uploaderAvatarUrl,
				uploader_comment = rawRow.uploaderComment
			)
			return@groupBy ComicsResponseItem(
				page = rawRow.page,
				comics_title = rawRow.title,
				image_url = rawRow.image,
				issue_name = rawRow.issueName,
				issue_date = rawRow.issueDate,
				uploader_data = uploaderData,
				comments = mutableListOf()
			)
		}.map { entry: Map.Entry<ComicsResponseItem, List<RawRowResult>> ->
			val comments = entry.value.filter {
				it.commentUserName != null &&
					it.commentUserAvatarUrl != null &&
					it.commentBody != null &&
					it.commentDate != null &&
					it.commentsId != null
			}.map { rawRowResult: RawRowResult ->
				CommentsResponseItem(
					user_name = rawRowResult.commentUserName!!,
					user_avatar_url = rawRowResult.commentUserAvatarUrl!!,
					user_status = rawRowResult.commentUserStatus,
					body = rawRowResult.commentBody!!,
					date = rawRowResult.commentDate!!,
					comments_id = rawRowResult.commentsId!!,
					edited_data = rawRowResult.editedText
				)
			}
			entry.key.comments.addAll(comments)
			entry.key.comments.sortBy { it.date }
			return@map entry.key
		}.sortedBy { it.page }
		val queryTme = queryTime - startTime
		val groupingTime = System.currentTimeMillis() - queryTime
		logger.info("query took $queryTme, grouping took $groupingTime")
		return resultList
	}
}

private data class RawRowResult(
	val page: Int,
	val title: String,
	val image: String,
	val issueName: String,
	val issueDate: Long,
	val uploaderName: String,
	val uploaderAvatarUrl: String,
	val uploaderComment: String?,
	// комменты могут отсутствовать, соответственно поля nullable.
	val commentsId: String?,
	val commentUserName: String?,
	val commentUserAvatarUrl: String?,
	val commentUserStatus: String?,
	val commentBody: String?,
	val commentDate: Long?,
	val editedText: String?
)