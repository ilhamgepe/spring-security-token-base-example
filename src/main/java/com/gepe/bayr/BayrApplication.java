package com.gepe.bayr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BayrApplication {

    public static void main(String[] args) {
        SpringApplication.run(BayrApplication.class, args);
    }

    // REMOVE THIS, IF NOT NEEDED
    @Bean
    public CommandLineRunner TestAja(@Value("${app.tink.keyset-json}") String keysetJson){
        return args -> {
            System.out.println("================ Tink Keyset JSON ================");
            System.out.println(keysetJson);
            System.out.println("==================================================");
        };
    }
}
