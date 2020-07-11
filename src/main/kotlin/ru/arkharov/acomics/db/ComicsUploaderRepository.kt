package ru.arkharov.acomics.db

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface ComicsUploaderRepository : JpaRepository<ComicsUploaderEntity, ComicsId> {
	
	@Transactional
	fun deleteByComicsId(comicsId: ComicsId)
}