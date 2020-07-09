package ru.arkharov.acomics.db

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface CommentsRepository : JpaRepository<CommentsEntity, Long> {
	
	@Transactional
	fun deleteByComics(comicsPageEntity: ComicsPageEntity)
}