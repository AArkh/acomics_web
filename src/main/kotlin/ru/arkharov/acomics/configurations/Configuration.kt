package ru.arkharov.acomics.configurations

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

private const val SINGLETON = "singleton"

@Configuration
class Configuration {
	
	@Bean
	@Scope(SINGLETON)
	fun webClient() : WebClient {
		return WebClient.builder()
			.baseUrl("https://acomics.ru")
			.defaultCookie("ageRestrict", "18")
			// see https://stackoverflow.com/questions/59326351/configure-spring-codec-max-in-memory-size-when-using-reactiveelasticsearchclient
			.exchangeStrategies(ExchangeStrategies.builder().codecs {
				it.defaultCodecs().maxInMemorySize(1024 * 1024 * 10)
			}.build())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
			.build()
	}
}