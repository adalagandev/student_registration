package com.studentregistration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CorsConfig — enables Cross-Origin Resource Sharing, the Java equivalent of
 * {@code CORS(app)} in app.py. The React dev server runs on
 * {@code http://localhost:5173} while this API runs on {@code :5000}; browsers block
 * cross-origin calls unless the server opts in with CORS headers.
 *
 * <p>Implementing {@link WebMvcConfigurer} and overriding {@code addCorsMappings}
 * registers the policy globally. We allow the Vite origin (from
 * {@code app.frontend-origin}) to use all the HTTP methods the frontend needs —
 * including the preflighted PUT/DELETE and the multipart POST — on any {@code /api/**}
 * route.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String frontendOrigin;

    public CorsConfig(@Value("${app.frontend-origin}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
