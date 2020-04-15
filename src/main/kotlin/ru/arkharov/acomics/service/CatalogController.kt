package ru.arkharov.acomics.service

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.arkharov.acomics.service.model.SearchResponseCatalogItem
import java.sql.ResultSet

@Component
@RestController
class CatalogController(
	private val jdbcTemplate: JdbcTemplate
) {
	
	private val logger = LoggerFactory.getLogger(this::class.java)
	private val validSortParams = arrayOf("last_updated", "total_subscribers", "total_pages", "title")
	
	@GetMapping("/search")
	fun search(
		@RequestParam(required = true) page: Int,
		@RequestParam(required = true) sortingBy: String,
		@RequestParam(required = true) isAsc: String,
		@RequestParam(defaultValue = "20") perPage: Int
	): List<SearchResponseCatalogItem> {
		logger.info("requested search with sort: $sortingBy")
		validateSortParam(sortingBy)
		validateIsAscParam(isAsc)
		validatePageParam(page)
		validateOffsetParam(perPage)
		val sort = sortingBy + if (isAsc.toBoolean()) "" else " DESC"
		val offsetForPage = perPage * (page - 1)
		return jdbcTemplate.query("""
			SELECT title, preview_image, description, rating, last_updated, total_pages, total_subscribers
			FROM catalog
			ORDER BY $sort
			LIMIT $perPage
			OFFSET $offsetForPage"""
		) { result: ResultSet, _ ->
			return@query SearchResponseCatalogItem(
				result.getString("title"),
				result.getString("preview_image"),
				result.getString("description"),
				result.getString("rating"),
				result.getLong("last_updated"),
				result.getInt("total_pages"),
				result.getInt("total_subscribers")
			)
		}
	}
	
	private fun validateSortParam(userInput: String) {
		if (!validSortParams.contains(userInput)) {
			throw IllegalArgumentException(
				"$userInput is not a valid param \"sortingBy\", use one of ${validSortParams.contentToString()}"
			)
		}
	}
	
	private fun validateIsAscParam(isAsc: String) {
		if (isAsc != "true" && isAsc != "false") {
			throw IllegalArgumentException("$isAsc is not a valid \"isAsc\" param, use true or false")
		}
	}
	
	private fun validatePageParam(page: Int) {
		if (page <= 0) {
			throw IllegalArgumentException("$page is not a valid \"page\" param, use positive number")
		}
	}
	
	private fun validateOffsetParam(perPage: Int) {
		if (perPage <= 0) {
			throw IllegalArgumentException("$perPage is not a valid \"perPage\" param, use positive number")
		}
	}
}

