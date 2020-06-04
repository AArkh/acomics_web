package ru.arkharov.acomics.parsing.catalog

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.arkharov.acomics.db.CatalogEntity
import java.net.URI
import java.text.ParseException
import java.util.regex.Pattern

private const val CATALOG_TABLE_ELEMENT_CLASS_NAME = "catalog-elem list-loadable"
private const val CATALOG_ELEMENT_PREVIEW_IMAGE_CLASS_NAME = "catdata1"
private const val CATALOG_ELEMENT_DESCRIPTION_CLASS_NAME = "catdata2"
private const val CATALOG_ELEMENT_ACTIVITY_CLASS_NAME = "catdata3"
private const val CATALOG_ELEMENT_SUBSCRIBERS_CLASS_NAME = "catdata4"
private const val BASE_ACOMICS_URL = "https://acomics.ru"

@Component
class CatalogHTMLParser {
	
	private val logger = LoggerFactory.getLogger(this::class.java)
	
	@Throws(ParseException::class)
	fun parse(html: String): List<CatalogEntity> {
		val doc: Document = Jsoup.parse(html)
		val elements = doc.getElementsByClass(CATALOG_TABLE_ELEMENT_CLASS_NAME)
		if (elements.isEmpty()) {
			logger.info("no elements in a list!")
			return emptyList()
		}
		
		val catalogList = ArrayList<CatalogEntity>(elements.size)
		
		elements.forEach {
			val htmlString = it.child(0)
			val previewElement = htmlString.getElementsByClass(CATALOG_ELEMENT_PREVIEW_IMAGE_CLASS_NAME)
				.first()
				.child(0)
			val comicLink:String = previewElement.attr("href")
			var catalogId = URI(comicLink).path.split("/").last()
			if (catalogId.first() == '~') {
				catalogId = catalogId.substring(1)
			}
			val imageLinkPostfix = previewElement.getElementsByTag("img").attr("src")
			val imageLink = BASE_ACOMICS_URL + imageLinkPostfix
			
			val descriptionElement = htmlString.getElementsByClass(CATALOG_ELEMENT_DESCRIPTION_CLASS_NAME)
				.first()
			val title = descriptionElement.getElementsByClass("title")
				.first()
				.child(0)
				.text()
			val description = descriptionElement.getElementsByClass("about")
				.first()
				.text()
			val rating = descriptionElement.getElementsByClass("also")
				.first()
				.getElementsByTag("a")
				.first()
				.text()
			
			val activityElement = htmlString.getElementsByClass(CATALOG_ELEMENT_ACTIVITY_CLASS_NAME)
				.first()
			val lastUpdate = activityElement.getElementsByClass("time")
				.first()
				.text()
				.replace("=", "")
				.toLong()
			val formattedTotalPages = activityElement.getElementsByClass("total")
				.first()
				.text()
			
			val subscribersElement = htmlString.getElementsByClass(CATALOG_ELEMENT_SUBSCRIBERS_CLASS_NAME)
				.first()
			val subsCount = subscribersElement.getElementsByClass("subscribe")
				.first()
				.child(0)
				.text()
				.toInt()
			
			val matcher = Pattern.compile("\\d+").matcher(formattedTotalPages)
			matcher.find()
			val totalPages = Integer.valueOf(matcher.group())
			
			val catalogItem = CatalogEntity(
				catalogId,
				comicLink,
				imageLink,
				title,
				description,
				rating,
				lastUpdate,
				totalPages,
				formattedTotalPages,
				0.0,
				subsCount
			)
			catalogList.add(catalogItem)
		}
		return catalogList
	}
}