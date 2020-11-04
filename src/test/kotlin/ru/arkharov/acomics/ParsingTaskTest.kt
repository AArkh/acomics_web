package ru.arkharov.acomics

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest
import org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTypeExcludeFilter
import org.springframework.data.repository.findByIdOrNull
import ru.arkharov.acomics.db.*
import ru.arkharov.acomics.parsing.ParsingTask
import ru.arkharov.acomics.parsing.comics.ParsedComicsPage

@DataJdbcTest
@AutoConfigureDataJpa
@TypeExcludeFilters(DataJpaTypeExcludeFilter::class)
@AutoConfigureTestEntityManager
class ParsingTaskTest {
	
	@field:Autowired
	private var task: ParsingTask? = null
	@field:Autowired
	private var catalogRepository: CatalogRepository? = null
	@field:Autowired
	private var comicsRepository: ComicsRepository? = null
	@field:Autowired
	private var comicsUploaderRepository: ComicsUploaderRepository? = null
	@field:Autowired
	private var commentsRepository: CommentsRepository? = null
	private val fakeCatalogItem = CatalogEntity(
		"LwHG",
		"https://acomics.ru/~LwHG",
		"https://acomics.ru/upload/b/b9142-8n3lv8b6yc.png",
		"Swedish Family Multiverse",
		"Парень, которого зовут Артур, снимает комнату, а его соседками оказались две де...",
		"R",
		1L,
		30,
		"",
		0.00,
		1
	)
	
	@Test
	fun comicsParserTest() {
		val catalogRepository = catalogRepository ?: throw IllegalStateException()
		val comicsRepository = comicsRepository ?: throw IllegalStateException()
		val comicsUploaderRepository = comicsUploaderRepository ?: throw IllegalStateException()
		val commentsRepository = commentsRepository ?: throw IllegalStateException()
		
		assert(catalogRepository.findByIdOrNull(fakeCatalogItem.catalogId) == null)
		assert(comicsRepository.findByCatalog(fakeCatalogItem).isEmpty())
		
		val task: ParsingTask = task ?: throw IllegalStateException()
		val list: List<ParsedComicsPage> = task.getComicsPages(fakeCatalogItem)
		
		assert(list.isNotEmpty())
		assert(list.size == fakeCatalogItem.totalPages)
		
		task.upsert(fakeCatalogItem, list)
	
		val catalogEntity = catalogRepository.findById(fakeCatalogItem.catalogId).get()
		
		assert(catalogEntity.hyperLink == fakeCatalogItem.hyperLink)
		assert(catalogEntity.rating.isNotEmpty())
		assert(catalogEntity.totalPages >= 0)
		assert(catalogEntity.catalogId == fakeCatalogItem.catalogId)
		assert(catalogEntity.title == fakeCatalogItem.title)
		assert(catalogEntity.description.isNotEmpty())
		assert(catalogEntity.previewImage.isNotEmpty())
		assert(catalogEntity.totalSubscribers > 0)
		assert(catalogEntity.lastUpdated > 0L)
		
		val comics: List<ComicsPageEntity> = comicsRepository.findByCatalog(catalogEntity)
		assert(comics.isNotEmpty())
		
		val uploaderComments = mutableListOf<String>()
		val comments = mutableListOf<CommentsEntity>()
		comics.forEach { comicsEntity: ComicsPageEntity ->
			assert(comicsEntity.catalog == catalogEntity)
			assert(comicsEntity.comicsId.page > 0)
			assert(comicsEntity.comicsId.catalogId == catalogEntity.catalogId)
			assert(comicsEntity.comicsTitle == catalogEntity.title)
			assert(comicsEntity.imageUrl.isNotEmpty())
			val uploaderData = comicsUploaderRepository.findById(comicsEntity.comicsId).get()
			
			assert(uploaderData.comicsId == comicsEntity.comicsId)
			assert(uploaderData.comicsPageEntity == comicsEntity)
			if (!uploaderData.commentBody.isNullOrEmpty()) {
				uploaderComments.add(uploaderData.commentBody!!)
			}
			assert(uploaderData.issueDate > 0L)
			assert(uploaderData.userName.isNotEmpty())
			assert(uploaderData.userProfileUrl.isNotEmpty())
			//todo here
			
			val commentsByComics = commentsRepository.findByComics(comicsEntity)
			if (commentsByComics.isNotEmpty()) {
				comments.addAll(commentsByComics)
				commentsByComics.forEach { commentsEntity: CommentsEntity ->
					assert(commentsEntity.commentId.isNotEmpty())
					assert(commentsEntity.comics == comicsEntity)
					assert(commentsEntity.date > 0L)
					assert(commentsEntity.commentBody.isNotEmpty())
					assert(commentsEntity.userName.isNotEmpty())
					assert(commentsEntity.userAvatarImageUrl.isNotEmpty())
				}
			}
		}
		
		assert(commentsRepository.findAll().any {
			it.comics.catalog.catalogId == fakeCatalogItem.catalogId
		})
		
		assert(uploaderComments.isNotEmpty())
		assert(comments.isNotEmpty())
		assert(comments.any {
			!it.userStatus.isNullOrEmpty()
		})
	}
}