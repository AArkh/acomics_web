package ru.arkharov.acomics.db

import org.springframework.data.jpa.repository.JpaRepository

interface CatalogRepository : JpaRepository<CatalogEntity, String>