package ru.arkharov.acomics.initiation

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

//todo kill??
@Component
class InitiationRunner : CommandLineRunner {
	
	private val logger = LoggerFactory.getLogger(this::class.java)
	
	override fun run(vararg args: String?) {
		logger.info("Application starting with arguments: ${args.toList()}")
	}
}