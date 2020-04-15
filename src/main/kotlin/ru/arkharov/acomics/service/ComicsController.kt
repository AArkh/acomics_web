package ru.arkharov.acomics.service

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import ru.arkharov.acomics.service.model.ComicsResponseItem
import java.sql.ResultSet

@Component
@RestController
class ComicsController (
	private val jdbcTemplate: JdbcTemplate
) {
	
	private val logger = LoggerFactory.getLogger(this::class.java)
	
	@GetMapping("/comics/{comicsTitle}")
	fun searchComics(@PathVariable(required = true) comicsTitle: String): List<ComicsResponseItem> {
		logger.info("requested comics with path: $comicsTitle")
		return jdbcTemplate.query("""
			SELECT comics_title, image_url, issue_name
			FROM comics
			WHERE comics_title = '$comicsTitle'
			ORDER BY image_url"""
		) { result: ResultSet, _ ->
			return@query ComicsResponseItem(
				result.getString("comics_title"),
				result.getString("image_url"),
				result.getString("issue_name")
			)
		}
	}
}