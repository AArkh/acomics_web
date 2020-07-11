package ru.arkharov.acomics.db

import javax.persistence.*

@Entity
@Table(name = COMMENTS_TABLE)
data class CommentsEntity(
	@field:Id
	@field:GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	val userName: String,
	val userStatus: String?, //user особый аттрибут пользователя. "Тайпер", "Переводчик", такие штуки...
	val userAvatarImageUrl: String,
	@field:Column(columnDefinition = "text")
	val commentBody: String,
	val date: Long,
	val commentId: String, //формата #1152325
	val editedData: String?, // формата "Отредактировано «RusPeanuts» 04.01.2020 02:19:54"
	@field:ManyToOne(fetch = FetchType.LAZY)
	val comics: ComicsPageEntity
)