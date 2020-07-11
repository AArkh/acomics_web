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

private const val COMICS_LINKS = "https://acomics.ru/~outsider"

@DataJdbcTest
@AutoConfigureDataJpa
@TypeExcludeFilters(DataJpaTypeExcludeFilter::class)
@AutoConfigureTestEntityManager
class RepositoryTests {
	
	@field:Autowired
	private var catalogRepository: CatalogRepository? = null
	@field:Autowired
	private var comicsRepository: ComicsRepository? = null
	@field:Autowired
	private var comicsUploaderRepository: ComicsUploaderRepository? = null
	@field:Autowired
	private var commentsRepository: CommentsRepository? = null
	
	private val catalogItem = CatalogEntity(
		"bearmageddon",
		"https://acomics.ru/~bearmageddon",
		"",
		"Swedish Family Multiverse",
		"Чот про медведей...",
		"R",
		1L,
		50,
		"",
		0.00,
		1
	)
	
	@Test
	fun comicsRepoTest() {
		val catalogRepository = this.catalogRepository ?: throw Exception("")
		val comicsRepository = this.comicsRepository ?: throw Exception("")
		val comicsUploaderRepository = this.comicsUploaderRepository ?: throw Exception("")
		val commentsRepository = this.commentsRepository ?: throw Exception("")
		
		val catalogEntity = catalogItem
		assert(catalogRepository.findByIdOrNull(catalogEntity.catalogId) == null)
		catalogRepository.save(catalogEntity)
		assert(catalogRepository.findByIdOrNull(catalogEntity.catalogId) != null)
		
		val fakeComicsPages = listOf(
			ComicsPageEntity(ComicsId(catalogEntity.catalogId, 1), "title1", "url1", "name1", catalogEntity),
			ComicsPageEntity(ComicsId(catalogEntity.catalogId, 2), "title2", "url2", "name2", catalogEntity),
			ComicsPageEntity(ComicsId(catalogEntity.catalogId, 3), "title3", "url3", "name3", catalogEntity),
			ComicsPageEntity(ComicsId(catalogEntity.catalogId, 4), "title4", "url4", "name4", catalogEntity),
			ComicsPageEntity(ComicsId(catalogEntity.catalogId, 5), "title5", "url5", "name5", catalogEntity),
			ComicsPageEntity(ComicsId(catalogEntity.catalogId, 6), "title6", "url6", "name6", catalogEntity),
			ComicsPageEntity(ComicsId(catalogEntity.catalogId, 7), "title7", "url1", "name7", catalogEntity),
			ComicsPageEntity(ComicsId(catalogEntity.catalogId, 2), "title3", "url1", "name16", catalogEntity)
		)
		
		val comicsUploaderDetailsEntities = fakeComicsPages.map {
			return@map ComicsUploaderEntity(
				it.comicsId,
				it,
				"name",
				"profile_url",
				0L,
				"body"
			)
		}
		
		var x = 0L
		val fakeComments = fakeComicsPages.flatMap {
			if (it.issueName == "name16") {
				return@flatMap emptyList<CommentsEntity>()
			}
			println("catalog id for ${it.comicsTitle} is ${it.catalog.catalogId}")
			return@flatMap listOf(
				CommentsEntity(
					id = x++,
					userName = "one",
					userStatus = null,
					userAvatarImageUrl = "ava",
					commentBody = "bodyone",
					date = 123123L,
					commentId = "${++x}",
					editedData = null,
					comics = it
				),
				CommentsEntity(
					id = x++,
					userName = "two",
					userStatus = null,
					userAvatarImageUrl = "ava2",
					commentBody = "bodytwo",
					date = 12312312L,
					commentId = "${++x}",
					editedData = null,
					comics = it
				)
			)
		}
		
		assert(catalogRepository.findByIdOrNull(catalogEntity.catalogId) != null)
		assert(comicsRepository.findByCatalog(catalogEntity).isEmpty())
		assert(!comicsUploaderRepository.findById(ComicsId(catalogEntity.catalogId, 1)).isPresent)
		comicsRepository.saveAll(fakeComicsPages)
		assert(comicsRepository.findByCatalog(catalogEntity).size == fakeComicsPages.size - 1)
		val comicsFromRepo = comicsRepository.findByCatalog(catalogEntity)
		comicsUploaderRepository.saveAll(comicsUploaderDetailsEntities)
		commentsRepository.saveAll(fakeComments)
		assert(comicsUploaderRepository.findAllById(comicsRepository.findByCatalog(catalogEntity).map { it.comicsId }).size == fakeComicsPages.size - 1)
		val commentsFromRepo = mutableListOf<CommentsEntity>()
		comicsFromRepo.forEach { comicsPage: ComicsPageEntity ->
			commentsFromRepo.addAll(commentsRepository.findByComics(comicsPage))
		}
		assert(commentsFromRepo.size == fakeComments.size)
		fakeComicsPages.forEach {
			comicsUploaderRepository.deleteByComicsId(it.comicsId)
			commentsRepository.deleteByComics(it)
		}
		comicsRepository.deleteByCatalog(catalogEntity)
		assert(comicsRepository.findByCatalog(catalogEntity).isEmpty())
		fakeComicsPages.forEach {
			assert(!comicsUploaderRepository.findById(it.comicsId).isPresent)
			assert(commentsRepository.findByComics(it).isEmpty())
		}
		assert(catalogRepository.findByIdOrNull(catalogEntity.catalogId) != null)
	}
}
