package ru.arkharov.acomics.parsing

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import ru.arkharov.acomics.db.CatalogEntity
import ru.arkharov.acomics.parsing.catalog.CatalogHTMLParser
import ru.arkharov.acomics.db.CatalogRepository
import ru.arkharov.acomics.parsing.comics.ComicsHTMLParser
import ru.arkharov.acomics.parsing.comics.ComicsPageData
import ru.arkharov.acomics.db.ComicsPageEntity
import ru.arkharov.acomics.db.ComicsRepository
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Function
import kotlin.IllegalStateException


private const val TEN_SECONDS: Long = 600 * 1000
private const val CATALOG_SKIP_QUERY_PARAM = "skip"
private const val CATALOG_CATEGORY_QUERY_PARAM = "categories"
private const val CATALOG_SORT_QUERY_PARAM = "sort"
private const val CATALOG_SORT_QUERY_BY_APLHABET = "serial_name" // other issue_count, subscr_count, last_update
private const val CATALOG_TRANSLATION_TYPE_QUERY_PARAM = "type"
private const val CATALOG_RATING_QUERY_PARAM = "ratings[]"
private const val CATALOG_UPDATABLE_QUERY_PARAM = "updatable"
private const val CATALOG_MINIMAL_ISSUES_COUNT_QUERY_PARAM = "issue_count"
private const val ACOMICS_DEFAULT_PAGINATION_OFFSET = 10 // Не умеет страничка иначе, поэтому не кастомизируем

@Component
class ParsingTask(
	private val webClient: WebClient,
	private val catalogParser: CatalogHTMLParser,
	private val comicsParser: ComicsHTMLParser,
	private val repository: CatalogRepository,
	private val comicsRepository: ComicsRepository
) {
	
	private val dateFormat = SimpleDateFormat("HH:mm:ss")
	private val logger = LoggerFactory.getLogger(this::class.java)
	
	private val ratingParams = arrayOf("1", "2", "3", "4", "5", "6")
	
	//todo сделать вилку на наличие данных в comics, чтобы не парсить все полностью
	//@Scheduled(fixedDelay = TEN_SECONDS)
	fun parse() {
		try {
			logger.info("started parsing at ${dateFormat.format(Date())}")
			var page = 0
			var catalogParsed = false
			while (!catalogParsed) {
				logger.info("parsing page ${page + 1}\n\n")
				val catalogHtml: String = retrieveCatalogHtml(page)
				val catalogPageEntries: List<CatalogEntity> = catalogParser.parse(catalogHtml)
				if (catalogPageEntries.isEmpty()) {
					catalogParsed = true
					continue
				}
				catalogPageEntries.forEach { catalogEntry: CatalogEntity ->
					val comicsEntries: List<ComicsPageEntity> = getComicsPages(catalogEntry)
					repository.save(catalogEntry)
					if (comicsEntries.isNotEmpty()) {
						comicsRepository.saveAll(comicsEntries)
					}
				}
				page++
			}
			logger.info("parsing ended at ${dateFormat.format(Date())}")
		} catch (e: Exception) {
			logger.error("parsing failed", e)
		}
	}
	
	private fun getComicsPages(catalogEntry: CatalogEntity): List<ComicsPageEntity> {
		if (catalogEntry.totalPages == 0) {
			logger.info("comics: ${catalogEntry.title} has no pages at all")
			return emptyList()
		}
		try {
			logger.info("parsing comics ${catalogEntry.title} pages at url ${catalogEntry.hyperLink}")
			val pageEntries: Array<ComicsPageEntity?> = arrayOfNulls(catalogEntry.totalPages)
			val pagesIndexesToParse: List<Int> = Array(catalogEntry.totalPages) { return@Array it }.toList()
			pagesIndexesToParse.parallelStream().forEach { index: Int ->
				val url = "${catalogEntry.hyperLink}/${index + 1}"
				val pageHtml = retrieveComicsPageHtml(url)
				val parsedComicsPage: ComicsPageData = comicsParser.parse(pageHtml)
				logger.info("successfully parsed page $url")
				pageEntries[index] = ComicsPageEntity(
					catalogEntry.title,
					parsedComicsPage.imageUrl,
					parsedComicsPage.issueName,
					catalogEntry
				)
			}
			val resultList: List<ComicsPageEntity> = pageEntries.filterNotNull()
			return if (resultList.size == pageEntries.size) {
				resultList
			} else throw IllegalStateException()
		} catch (e: Exception) {
			logger.error("failed to parse some pages, returning empty result for ${catalogEntry.title}")
			return emptyList()
		}
	}
	
	@Throws(IllegalStateException::class)
	private fun retrieveComicsPageHtml(comicsPageUrl: String): String {
		val resp = webClient
			.get()
			.uri(comicsPageUrl)
			.retrieve()
		return resp.bodyToMono(String::class.java).block()
			?: throw IllegalStateException("no body!")
	}
	
	@Throws(IllegalStateException::class)
	private fun retrieveCatalogHtml(page: Int): String {
		return webClient
			.get()
			.uri(Function { builder: UriBuilder ->
				return@Function builder
					.path("/comics")
					.queryParam(CATALOG_CATEGORY_QUERY_PARAM, "")
					.queryParam(CATALOG_RATING_QUERY_PARAM, *ratingParams)
					.queryParam(CATALOG_TRANSLATION_TYPE_QUERY_PARAM, TranslationType.ANY.queryParam)
					.queryParam(CATALOG_SORT_QUERY_PARAM, CATALOG_SORT_QUERY_BY_APLHABET)
					.queryParam(CATALOG_UPDATABLE_QUERY_PARAM, "0")
					.queryParam(CATALOG_MINIMAL_ISSUES_COUNT_QUERY_PARAM, "0")
					.queryParam(CATALOG_SKIP_QUERY_PARAM, "${page * ACOMICS_DEFAULT_PAGINATION_OFFSET}")
					.build()
			})
			.retrieve()
			.bodyToMono(String::class.java)
			.block()
			?: throw IllegalStateException("no body!")
	}
}

enum class TranslationType(val queryParam: String) {
	ANY("0"),
	ORIGINAL("orig"),
	TRANSLATED("trans")
}
