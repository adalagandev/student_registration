package com.studentregistration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StudentRegistrationApplication — the entry point of the Java backend.
 *
 * <p>This is the Spring Boot equivalent of the {@code if __name__ == "__main__"}
 * block at the bottom of {@code app.py}. Running {@code main} boots an embedded
 * Tomcat web server, wires up every {@code @Component}/{@code @Service}/
 * {@code @RestController} in this package (and sub-packages), and starts listening
 * on the port from {@code application.properties} (5000).
 *
 * <p>The single annotation below does a lot of work:
 * <ul>
 *   <li>{@code @SpringBootApplication} is a convenience meta-annotation combining
 *       {@code @Configuration} (this class can define beans),
 *       {@code @EnableAutoConfiguration} (Spring guesses sensible setup from the
 *       libraries on the classpath — e.g. seeing spring-data-jpa it configures a
 *       {@code DataSource} and Hibernate), and {@code @ComponentScan} (it discovers
 *       our classes automatically, so there is no manual route registration like
 *       Flask's {@code @app.route}).
 * </ul>
 *
 * <p>Note we do NOT call an explicit {@code init_db()} here: Hibernate's
 * {@code ddl-auto=update} creates/migrates the schema on startup, and the uploads
 * directory is created lazily by {@code FileStorageService} when the first file is
 * saved.
 */
@SpringBootApplication
public class StudentRegistrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentRegistrationApplication.class, args);
    }
}
