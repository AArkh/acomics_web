package ru.arkharov.acomics.parsing

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import ru.arkharov.acomics.db.*
import ru.arkharov.acomics.parsing.catalog.CatalogHTMLParser
import ru.arkharov.acomics.parsing.comics.ComicsHTMLParser
import ru.arkharov.acomics.parsing.comics.ParsedComicsComment
import ru.arkharov.acomics.parsing.comics.ParsedComicsPage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
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
	private val comicsRepository: ComicsRepository,
	private val comicsUploaderRepository: ComicsUploaderRepository,
	private val commentsRepository: CommentsRepository
) {
	
	private val dateFormat = SimpleDateFormat("HH:mm:ss")
	private val logger = LoggerFactory.getLogger(this::class.java)
	private val ratingParams = arrayOf("1", "2", "3", "4", "5", "6")
	private val block = Any()
	
	@Scheduled(fixedDelay = HOUR)
	fun parse() {
		if (comicsRepository.count() >= 0) {
			logger.info("Updating starts")
			updateParsingTask()
		} else {
			logger.info("Initial parsing starts")
			initialParseTask()
		}
	}
	
	fun updateParsingTask() {
		var updatedCatalogItems = 0
		var updatedComicsItems = 0
		var page = 0
		try {
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
					val optionalSavedEntry: Optional<CatalogEntity> = catalogRepository.findById(catalogEntry.catalogId)
					if (!optionalSavedEntry.isPresent) {
						logger.info("found new comics of title: ${catalogEntry.title}, catalogId: ${catalogEntry.catalogId}")
						val parsedComicsEntries: List<ParsedComicsPage> = getComicsPages(catalogEntry)
						updatedCatalogItems++
						updatedComicsItems += parsedComicsEntries.size
						upsert(catalogEntry, parsedComicsEntries)
					} else {
						val savedEntry: CatalogEntity = optionalSavedEntry.get()
						if (savedEntry.olderThan(catalogEntry)) {
							logger.info("${catalogEntry.title} is changed, updating whole entry")
							val parsedComicsEntries: List<ParsedComicsPage> = getComicsPages(catalogEntry)
							updatedCatalogItems++
							updatedComicsItems += parsedComicsEntries.size
							upsert(catalogEntry, parsedComicsEntries)
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
		var page = 0
		val startTime = System.currentTimeMillis()
		try {
			logger.info("started parsing at ${dateFormat.format(Date())}")
			var catalogParsed = false
			while (!catalogParsed) {
				logger.info("\nparsing page ${page + 1}\n")
				val catalogHtml: String = retrieveCatalogHtml(page, CATALOG_SORT_QUERY_BY_APLHABET)
				val catalogPageEntries: List<CatalogEntity> = catalogParser.parse(catalogHtml)
				if (catalogPageEntries.isEmpty()) {
					catalogParsed = true
					continue
				}
				catalogPageEntries.parallelStream().forEach { catalogEntry: CatalogEntity ->
					val parsedComicsEntries: List<ParsedComicsPage> = getComicsPages(catalogEntry)
					logger.info("found comics of title: ${catalogEntry.title}, catalogId: ${catalogEntry.catalogId}")
					upsert(catalogEntry, parsedComicsEntries)
				}
				page++
			}
			val finishTime = System.currentTimeMillis()
			val resultParsingTime = TimeUnit.MILLISECONDS.toMinutes(startTime - finishTime)
			logger.info("parsing ended at ${dateFormat.format(Date())}, and took $resultParsingTime minutes")
		} catch (e: Exception) {
			logger.error("initial parsing failed at page $page", e)
		}
	}
	
	fun upsert(catalogEntry: CatalogEntity, comicsParsedPages: List<ParsedComicsPage>) {
		val commentsEntities: MutableList<CommentsEntity> = LinkedList()
		val comicsUploaderEntities: MutableList<ComicsUploaderEntity> = LinkedList()
		val comicsEntries: List<ComicsPageEntity> = comicsParsedPages.mapIndexed { index, comicsPageData ->
			val comicsEntity = ComicsPageEntity(
				ComicsId(catalogEntry.catalogId, index.inc()),
				catalogEntry.title,
				comicsPageData.imageUrl,
				comicsPageData.issueName,
				catalogEntry
			)
			comicsUploaderEntities.add(
				ComicsUploaderEntity(
					comicsId = comicsEntity.comicsId,
					comicsPageEntity = comicsEntity,
					userName = comicsPageData.parsedUploaderComment.userName,
					userProfileUrl = comicsPageData.parsedUploaderComment.userProfileUrl,
					issueDate = comicsPageData.parsedUploaderComment.issueDate.toLongOrNull() ?: -1L,
					commentBody = comicsPageData.parsedUploaderComment.commentBody
				)
			)
			commentsEntities.addAll(comicsPageData.comments.map { parsedComicsComment: ParsedComicsComment ->
				CommentsEntity(
					userName = parsedComicsComment.userName,
					userStatus = parsedComicsComment.userStatus,
					userAvatarImageUrl = parsedComicsComment.userProfileUrl,
					commentBody = parsedComicsComment.commentBody,
					date = parsedComicsComment.date.toLongOrNull() ?: -1L,
					commentId = parsedComicsComment.commentId,
					editedData = parsedComicsComment.editedData,
					comics = comicsEntity
				)
			})
			return@mapIndexed comicsEntity
		}
		upsertToDb(catalogEntry, comicsEntries, comicsUploaderEntities, commentsEntities)
	}
	
	@Transactional
	fun upsertToDb(
		catalogEntry: CatalogEntity,
		comicsEntries: List<ComicsPageEntity>,
		comicsUploaderEntries: List<ComicsUploaderEntity>,
		commentsEntity: List<CommentsEntity>
	) {
		synchronized(block) {
			val startTime = System.currentTimeMillis()
			catalogRepository.save(catalogEntry)
			if (comicsEntries.isNotEmpty()) {
				val previousComicsList = comicsRepository.findByCatalog(catalogEntry)
				previousComicsList.forEach { previousComics: ComicsPageEntity ->
					commentsRepository.deleteByComics(previousComics)
					comicsUploaderRepository.deleteByComicsId(previousComics.comicsId)
				}
				comicsRepository.deleteByCatalog(catalogEntry)
				comicsRepository.saveAll(comicsEntries)
				commentsRepository.saveAll(commentsEntity)
				comicsUploaderRepository.saveAll(comicsUploaderEntries)
			}
			val finishTime = System.currentTimeMillis()
			logger.info("db updated for ${finishTime - startTime}ms, for ${catalogEntry.catalogId}")
		}
	}
	
	fun getComicsPages(catalogEntry: CatalogEntity): List<ParsedComicsPage> {
		if (catalogEntry.totalPages == 0) {
			logger.info("comics: ${catalogEntry.title} has no pages at all, catalog url: ${catalogEntry.hyperLink}")
			return emptyList()
		}
		try {
			val pageEntryParseds: Array<ParsedComicsPage?> = arrayOfNulls(catalogEntry.totalPages)
			val pagesIndexesToParse: List<Int> = Array(catalogEntry.totalPages) { return@Array it }.toList()
			pagesIndexesToParse.parallelStream().forEach { index: Int ->
				val url = "${catalogEntry.hyperLink}/${index + 1}"
				val pageHtml = retrieveComicsPageHtml(url)
				pageEntryParseds[index] = comicsParser.parse(pageHtml)
			}
			return pageEntryParseds.toList() as List<ParsedComicsPage>
		} catch (e: Exception) {
			logger.error("failed to parse some pages, returning empty result for ${catalogEntry.title}", e)
			return emptyList()
		}
	}
	
	@Throws(IllegalStateException::class)
	private fun retrieveCatalogHtml(page: Int, sortType: String): String {
		return tryTwice {
			webClient.get()
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
	}
	
	@Throws(IllegalStateException::class)
	private fun retrieveComicsPageHtml(comicsPageUrl: String): String {
		return tryTwice {
			val resp = webClient
				.get()
				.uri(comicsPageUrl)
				.retrieve()
			resp.bodyToMono<String>(String::class.java).block() ?: throw IllegalStateException("no body!")
		}
	}
	
	// Периодически 500 стреляют, для длинных комиксов критично. Идиотизьм! Переделуть на проверку пятисоточек!
	private fun <TYPE> tryTwice(block: () -> TYPE): TYPE {
		return try {
			block.invoke()
		} catch (e: Exception) {
			block.invoke()
		}
	}
	
	private fun CatalogEntity.olderThan(other: CatalogEntity): Boolean {
		return this.totalPages < other.totalPages || this.hyperLink != other.hyperLink
	}
}