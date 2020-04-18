package ru.arkharov.acomics.configurations

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

private const val SINGLETON = "singleton"

@Configuration
class Configuration {
	
	@Bean
	@Scope(SINGLETON)
	fun webClient() : WebClient {
		val logger = LoggerFactory.getLogger(WebClient::class.java)
		return WebClient.builder()
			.baseUrl("https://acomics.ru")
			.defaultCookie("ageRestrict", "17")
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
			.build()
	}
}