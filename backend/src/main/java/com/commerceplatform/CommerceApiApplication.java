package com.commerceplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class CommerceApiApplication {
    public static void main(String[] args) {
        var application = new SpringApplication(CommerceApiApplication.class);
        // Existing installations may still keep their embedded database under its original name.
        if (!Files.exists(Path.of("data", "commerce.mv.db")) && Files.isRegularFile(Path.of("data", "thar-nectar.mv.db"))) {
            application.setDefaultProperties(Map.of("commerce.local-database", "thar-nectar"));
        }
        application.run(args);
        System.out.println("The server started \n\n\n ********** Apllication Started *************** ");

    }
}
