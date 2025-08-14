package com.concesur.inventario_pro.config;


import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.Getter;

@Configuration
@PropertySource("classpath:inventario.properties")
@Getter
public class AppConfig {

    @Bean
    @Primary
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
// RestTemplate sin redirecciones
    @Bean
    @Qualifier("restTemplateSinRedirect")
    RestTemplate restTemplateSinRedirect() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }

    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:8080")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    @Value("${app.go.publicadoInv.tab}")
    private String publicadoInvTab;
    @Value("${app.go.obtenerVehicleStockId.tab}")
    private String obtenerVehicleIdTab;
    @Value("${app.g.obtenerResponsableReserva.tab}")
    private String obtenerResponsableReservaTab;
    @Value("${app.go.user}")
    private String user;
    @Value("${app.go.password}")
    private String password;
    @Value("${app.url.token}")
    private String urlToken;
}
