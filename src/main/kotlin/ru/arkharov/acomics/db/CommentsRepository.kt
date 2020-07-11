package ru.arkharov.acomics.db

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface CommentsRepository : JpaRepository<CommentsEntity, Long> {
	
	@Transactional
	fun findByComics(comics: ComicsPageEntity): List<CommentsEntity>
	
	@Transactional
	fun deleteByComics(comicsPageEntity: ComicsPageEntity)
}