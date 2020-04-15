package ru.arkharov.acomics.configurations

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.http.client.reactive.ClientHttpResponse
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import java.util.function.Function
import javax.sql.DataSource

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
			.filter { request, next ->
				logger.info("making request to ${request.url()}")
				return@filter next.exchange(request)
			}
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
			.build()
	}
}