package ru.arkharov.acomics.db

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.arkharov.acomics.service.model.SearchResponseCatalogItem

interface CatalogRepository : JpaRepository<CatalogEntity, String>