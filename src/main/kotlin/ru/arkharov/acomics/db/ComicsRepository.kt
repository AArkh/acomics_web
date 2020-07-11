package ru.arkharov.acomics.db

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface ComicsRepository : JpaRepository<ComicsPageEntity, ComicsId> {

	@Transactional
	fun findByCatalog(catalogEntity: CatalogEntity): List<ComicsPageEntity>
	
	@Transactional
	fun deleteByCatalog(catalogEntity: CatalogEntity): Long
}