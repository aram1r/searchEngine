package searchengine.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "html-parser-service-impl")
public class AppProps {
    private String userAgent;
    private String referrer;
}
