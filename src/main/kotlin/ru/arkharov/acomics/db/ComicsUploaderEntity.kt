package ru.arkharov.acomics.db

import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name = COMICS_UPLOADER_TABLE)
data class ComicsUploaderEntity(
	@field:Id
	val comicsId: ComicsId,
	@field:OneToOne(fetch = FetchType.LAZY)
	@field:MapsId
	val comicsPageEntity: ComicsPageEntity,
	val userName: String,
	val userProfileUrl: String,
	val issueDate: Long,
	@field:Column(columnDefinition = "text")
	val commentBody: String?
): Serializable