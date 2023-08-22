package io.github.booster.example.fulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.autoconfig.instrument.kafka.TracingReactorKafkaAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = TracingReactorKafkaAutoConfiguration.class)
public class FulfillmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FulfillmentApplication.class, args);
    }
}
