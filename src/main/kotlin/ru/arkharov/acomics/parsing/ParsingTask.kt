package ru.arkharov.acomics.parsing

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import ru.arkharov.acomics.db.CatalogEntity
import ru.arkharov.acomics.db.CatalogRepository
import ru.arkharov.acomics.db.ComicsPageEntity
import ru.arkharov.acomics.db.ComicsRepository
import ru.arkharov.acomics.parsing.catalog.CatalogHTMLParser
import ru.arkharov.acomics.parsing.comics.ComicsHTMLParser
import ru.arkharov.acomics.parsing.comics.ComicsPageData
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Function


private const val HOUR: Long = 60 * 60 * 1000
private const val CATALOG_SKIP_QUERY_PARAM = "skip"
private const val CATALOG_CATEGORY_QUERY_PARAM = "categories"
private const val CATALOG_SORT_QUERY_PARAM = "sort"
private const val CATALOG_SORT_QUERY_BY_APLHABET = "serial_name" // other issue_count, subscr_count, last_update
private const val CATALOG_SORT_QUERY_BY_LAST_UPDATE = "serial_name" // other issue_count, subscr_count, last_update
private const val CATALOG_TRANSLATION_TYPE_QUERY_PARAM = "type"
private const val CATALOG_TRANSLATION_TYPE_ANY = "type" // other orig, trans
private const val CATALOG_RATING_QUERY_PARAM = "ratings[]"
private const val CATALOG_UPDATABLE_QUERY_PARAM = "updatable"
private const val CATALOG_MINIMAL_ISSUES_COUNT_QUERY_PARAM = "issue_count"
private const val ACOMICS_DEFAULT_PAGINATION_OFFSET = 10 // Не умеет страничка иначе, поэтому не кастомизируем

@Component
class ParsingTask(
	private val webClient: WebClient,
	private val catalogParser: CatalogHTMLParser,
	private val comicsParser: ComicsHTMLParser,
	private val catalogRepository: CatalogRepository,
	private val comicsRepository: ComicsRepository
) {
	
	private val dateFormat = SimpleDateFormat("HH:mm:ss")
	private val logger = LoggerFactory.getLogger(this::class.java)
	
	private val ratingParams = arrayOf("1", "2", "3", "4", "5", "6")
	
	@Scheduled(fixedDelay = HOUR)
	fun parse() {
		if (comicsRepository.count() >= 0) {
			updateParsingTask()
		} else {
			initialParseTask()
		}
	}
	
	fun updateParsingTask() {
		var updatedCatalogItems = 0
		var updatedComicsItems = 0
		var page = 0
		try {
			logger.info("started parsing at ${dateFormat.format(Date())}")
			var catalogParsed = false
			while (!catalogParsed) {
				logger.info("parsing page ${page + 1}\n\n")
				val catalogHtml: String = retrieveCatalogHtml(page, CATALOG_SORT_QUERY_BY_LAST_UPDATE)
				val catalogPageEntries: List<CatalogEntity> = catalogParser.parse(catalogHtml)
				if (catalogPageEntries.isEmpty()) {
					catalogParsed = true
					continue
				}
				catalogPageEntries.parallelStream().forEach { catalogEntry: CatalogEntity ->
					val optionalSavedEntry: Optional<CatalogEntity> = catalogRepository.findById(catalogEntry.title)
					if (!optionalSavedEntry.isPresent) {
						logger.info("found new comics of title: ${catalogEntry.title}")
						val comicsEntries: List<ComicsPageEntity> = getComicsPages(catalogEntry)
						updatedCatalogItems++
						updatedComicsItems += comicsEntries.size
						upsert(catalogEntry, comicsEntries)
					} else {
						val savedEntry: CatalogEntity = optionalSavedEntry.get()
						if (savedEntry.olderThan(catalogEntry)) {
							logger.info("${catalogEntry.title} is changed, updating whole entry")
							val comicsEntries: List<ComicsPageEntity> = getComicsPages(catalogEntry)
							updatedCatalogItems++
							updatedComicsItems += comicsEntries.size
							upsert(catalogEntry, comicsEntries)
						} else {
							logger.info("${catalogEntry.title} is up to date")
							updatedCatalogItems++
							upsert(catalogEntry, emptyList())
						}
					}
				}
				page++
			}
			logger.info("parsing ended at ${dateFormat.format(Date())}")
		} catch (e: Exception) {
			logger.error("parsing failed", e)
		}
		logger.info("updated $updatedCatalogItems comics of $updatedComicsItems pages total")
	}
	
	fun initialParseTask() {
		logger.info("initial parsing in progress")
		var page = 0
		try {
			logger.info("started parsing at ${dateFormat.format(Date())}")
			var catalogParsed = false
			while (!catalogParsed) {
				logger.info("parsing page ${page + 1}\n\n")
				val catalogHtml: String = retrieveCatalogHtml(page, CATALOG_SORT_QUERY_BY_APLHABET)
				val catalogPageEntries: List<CatalogEntity> = catalogParser.parse(catalogHtml)
				if (catalogPageEntries.isEmpty()) {
					catalogParsed = true
					continue
				}
				catalogPageEntries.parallelStream().forEach { catalogEntry: CatalogEntity ->
					val comicsEntries: List<ComicsPageEntity> = getComicsPages(catalogEntry)
					upsert(catalogEntry, comicsEntries)
				}
				page++
			}
			logger.info("parsing ended at ${dateFormat.format(Date())}")
		} catch (e: Exception) {
			logger.error("initial parsing failed at page $page", e)
		}
	}
	
	@Transactional
	fun upsert(catalogEntry: CatalogEntity, comicsEntries: List<ComicsPageEntity>) {
		catalogRepository.save(catalogEntry)
		if (comicsEntries.isNotEmpty()) {
			comicsRepository.saveAll(comicsEntries)
		}
	}
	
	private fun getComicsPages(catalogEntry: CatalogEntity): List<ComicsPageEntity> {
		if (catalogEntry.totalPages == 0) {
			logger.info("comics: ${catalogEntry.title} has no pages at all")
			return emptyList()
		}
		try {
			val pageEntries: Array<ComicsPageEntity?> = arrayOfNulls(catalogEntry.totalPages)
			val pagesIndexesToParse: List<Int> = Array(catalogEntry.totalPages) { return@Array it }.toList()
			pagesIndexesToParse.parallelStream().forEach { index: Int ->
				val url = "${catalogEntry.hyperLink}/${index + 1}"
				val pageHtml = retrieveComicsPageHtml(url)
				val parsedComicsPage: ComicsPageData = comicsParser.parse(pageHtml)
				pageEntries[index] = ComicsPageEntity(
					catalogEntry.title,
					parsedComicsPage.imageUrl,
					parsedComicsPage.issueName,
					catalogEntry
				)
			}
			return pageEntries.toList() as List<ComicsPageEntity>
		} catch (e: Exception) {
			logger.error("failed to parse some pages, returning empty result for ${catalogEntry.title}")
			return emptyList()
		}
	}
	
	@Throws(IllegalStateException::class)
	private fun retrieveCatalogHtml(page: Int, sortType: String): String {
		return webClient
			.get()
			.uri(Function { builder: UriBuilder ->
				return@Function builder
					.path("/comics")
					.queryParam(CATALOG_CATEGORY_QUERY_PARAM, "")
					.queryParam(CATALOG_RATING_QUERY_PARAM, *ratingParams)
					.queryParam(CATALOG_TRANSLATION_TYPE_QUERY_PARAM, CATALOG_TRANSLATION_TYPE_ANY)
					.queryParam(CATALOG_SORT_QUERY_PARAM, sortType)
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
	
	@Throws(IllegalStateException::class)
	private fun retrieveComicsPageHtml(comicsPageUrl: String): String {
		val resp = webClient
			.get()
			.uri(comicsPageUrl)
			.retrieve()
		return resp.bodyToMono(String::class.java).block()
			?: throw IllegalStateException("no body!")
	}
	
	private fun CatalogEntity.olderThan(other: CatalogEntity): Boolean {
		return this.totalPages < other.totalPages || this.hyperLink != other.hyperLink
	}
}