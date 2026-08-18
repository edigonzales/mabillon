package guru.interlis.mabillon;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MockMvcSecurityTestConfiguration {

    @Bean
    MockMvcBuilderCustomizer mabillonSecurityMockMvcBuilderCustomizer() {
        return builder -> builder.apply(springSecurity());
    }
}
