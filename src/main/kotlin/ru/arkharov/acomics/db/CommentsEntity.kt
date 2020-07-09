package ru.arkharov.acomics.db

import javax.persistence.*

@Entity
@Table(name = COMMENTS_TABLE)
data class CommentsEntity(
	@field:Id
	@field:GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	val userName: String,
	val userStatus: String?, // особый аттрибут пользователя. "Тайпер", "Переводчик", такие штуки...
	val userAvatarImageUrl: String,
	@Column(columnDefinition = "text")
	val commentBody: String,
	val formattedDate: String, // "2 марта" или "15 марта 2019 года"
	@field:ManyToOne(fetch = FetchType.LAZY)
	val comics: ComicsPageEntity
)