package io.zeebe.cloud.events.router;

import io.zeebe.spring.client.EnableZeebeClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableZeebeClient
public class ZeebeCloudEventsRouterApp {

    public static void main(String[] args) {
        SpringApplication.run(ZeebeCloudEventsRouterApp.class, args);
    }

}
