package ru.arkharov.acomics.service

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.arkharov.acomics.service.model.SearchResponseCatalogItem
import java.sql.ResultSet

private const val DEFAULT_SEARCH_LIMIT = 20

@Component
@RestController
class SearchController(
	private val jdbcTemplate: JdbcTemplate
) {
	
	private val logger = LoggerFactory.getLogger(this::class.java)
	
	@GetMapping("/search/query")
	fun searchQuery(
		@RequestParam(required = true) mask: String
	): List<SearchResponseCatalogItem> {
		logger.info("requested search with mask: $mask")
		validateSearchMask(mask)
		val formattedMask = "%$mask%"
		val query = """
			SELECT catalog_id, title, preview_image, description, rating, last_updated, total_pages, total_subscribers
			FROM catalog
			WHERE title LIKE '$formattedMask'
			LIMIT $DEFAULT_SEARCH_LIMIT
			"""
		logger.info(query)
		
		try {
			return jdbcTemplate.query(query) { result: ResultSet, _ ->
				return@query SearchResponseCatalogItem(
					result.getString("catalog_id"),
					result.getString("title"),
					result.getString("preview_image"),
					result.getString("description"),
					result.getString("rating"),
					result.getLong("last_updated"),
					result.getInt("total_pages"),
					result.getInt("total_subscribers")
				)
			}
		} catch (e: Exception) {
			throw IllegalArgumentException("failed to search with mask: $mask")
		}
	}
	
	private fun validateSearchMask(mask: String) {
		require(mask.length < 10) {
			"mask length shouldn't exceed 10 chars"
		}
	}
}