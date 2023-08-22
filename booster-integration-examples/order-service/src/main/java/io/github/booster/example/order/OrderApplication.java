package io.github.booster.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.autoconfig.instrument.kafka.TracingReactorKafkaAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = TracingReactorKafkaAutoConfiguration.class)
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
