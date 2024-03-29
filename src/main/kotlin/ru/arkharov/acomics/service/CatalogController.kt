package ru.arkharov.acomics.service

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.arkharov.acomics.db.MPAARating
import ru.arkharov.acomics.service.model.SearchResponseCatalogItem
import java.sql.ResultSet

@Component
@RestController
class CatalogController(
	private val jdbcTemplate: JdbcTemplate
) {
	
	private val logger = LoggerFactory.getLogger(this::class.java)
	private val validSortParams = arrayOf("last_updated", "total_subscribers", "total_pages", "title")
	private val validRatingParams = arrayOf(*MPAARating.values().map { it.stringValue }.toTypedArray())
	
	@GetMapping("/search")
	fun search(
		@RequestParam(required = true) page: Int,
		@RequestParam(required = true) sortingBy: String,
		@RequestParam(required = true) isAsc: String,
		@RequestParam(defaultValue = "20") perPage: Int,
		@RequestParam(required = false) rating: Array<String>?,
		@RequestParam(required = false) minPages: Int?
	): List<SearchResponseCatalogItem> {
		logger.info("requested search with sort: $sortingBy")
		validateSortParam(sortingBy)
		validateIsAscParam(isAsc)
		validatePageParam(page)
		validateOffsetParam(perPage)
		validateRatingParam(rating)
		validateMinPagesParam(minPages)
		val sort = sortingBy + if (isAsc.toBoolean()) "" else " DESC"
		val offsetForPage = perPage * (page - 1)
		val where = formRatingQueryPart(rating, minPages)
		return jdbcTemplate.query("""
			SELECT catalog_id, title, preview_image, description, rating, last_updated, total_pages, total_subscribers
			FROM catalog
			$where
			ORDER BY $sort
			LIMIT $perPage
			OFFSET $offsetForPage"""
		) { result: ResultSet, _ ->
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
	}
	
	private fun validateSortParam(userInput: String) {
		if (!validSortParams.contains(userInput)) {
			throw IllegalArgumentException(
				"$userInput is not a valid param \"sortingBy\", use one of ${validSortParams.joinToString()}"
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
	
	private fun validateRatingParam(rating: Array<String>?) {
		if (rating == null) {
			return
		}
		rating.forEach { rate: String ->
			if (!validRatingParams.contains(rate.toUpperCase())) {
				throw IllegalArgumentException(
					"$rate is not a valid \"rating\" param, use on of ${validRatingParams.joinToString()}"
				)
			}
		}
	}
	
	private fun validateMinPagesParam(minPages: Int?) {
		if (minPages != null && minPages <= 0) {
			throw IllegalArgumentException("$minPages is not a valid \"minPages\" param, use positive number")
		}
	}
	
	private fun formRatingQueryPart(rating: Array<String>?, minPages: Int?): String {
		val ratingPart = if (!rating.isNullOrEmpty()) {
			"rating IN('${rating.joinToString("', '")}')"
		} else ""
		val minPagesPart = if (minPages != null) {
			"total_pages >= $minPages"
		} else ""
		if (ratingPart.isEmpty() && minPagesPart.isEmpty()) {
			return ""
		}
		return if (ratingPart.isNotEmpty() xor minPagesPart.isNotEmpty()) {
			"WHERE $ratingPart$minPagesPart"
		} else "WHERE $ratingPart AND $minPagesPart"
	}
}

