package ru.arkharov.acomics.db

import org.springframework.data.jpa.repository.JpaRepository

interface ComicsRepository : JpaRepository<ComicsPageEntity, String>