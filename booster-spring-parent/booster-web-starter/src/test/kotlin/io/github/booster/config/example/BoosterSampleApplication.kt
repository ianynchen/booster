package io.github.booster.config.example

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class BoosterSampleApplication
fun main(args: Array<String>) {
    SpringApplication.run(BoosterSampleApplication::class.java, *args)
}
