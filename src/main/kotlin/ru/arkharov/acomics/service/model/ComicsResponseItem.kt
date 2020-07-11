package ru.arkharov.acomics.service.model

class ComicsResponseItem(
	val page: Int,
	val comics_title: String,
	val image_url: String,
	val issue_name: String, // Название главы или странички, может отсутствовать.
	val issue_date: Long,
	val uploader_data: UploaderData,
	val comments: MutableList<CommentsResponseItem>
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as ComicsResponseItem
		if (page != other.page) return false
		if (comics_title != other.comics_title) return false
		if (image_url != other.image_url) return false
		if (issue_name != other.issue_name) return false
		if (issue_date != other.issue_date) return false
		if (uploader_data != other.uploader_data) return false
		return true
	}
	
	override fun hashCode(): Int {
		var result = page
		result = 31 * result + comics_title.hashCode()
		result = 31 * result + image_url.hashCode()
		result = 31 * result + issue_name.hashCode()
		result = 31 * result + issue_date.hashCode()
		result = 31 * result + uploader_data.hashCode()
		return result
	}
}

data class UploaderData(
	val uploader_name: String,
	val uploader_avatar_url: String,
	val uploader_comment: String?
)

data class CommentsResponseItem(
	val user_name: String,
	val user_avatar_url: String,
	val user_status: String?,
	val body: String,
	val date: Long,
	val comments_id: String,
	val edited_data: String?
)