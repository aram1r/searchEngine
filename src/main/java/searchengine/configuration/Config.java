package searchengine.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import searchengine.services.indexService.taskPools.URLTaskPool;

@Configuration
public class Config {
    @Bean
    public URLTaskPool taskPool () {
        return new URLTaskPool();
    }

    @Bean
    public AppProps appProps() {
        return new AppProps();
    }
}
