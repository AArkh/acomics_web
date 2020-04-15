package ru.arkharov.acomics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableJpaRepositories
@EntityScan
@SpringBootApplication
class AcomicsApplication

fun main(args: Array<String>) {
	val context = runApplication<AcomicsApplication>(*args)
}
