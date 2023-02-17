package searchengine.configuration;

import org.apache.logging.log4j.jul.LogManager;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import searchengine.Application;
import searchengine.services.indexService.taskPools.URLTaskPool;

import java.io.IOException;

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

//    @Bean
//    public LuceneMorphology luceneMorphology() throws IOException {
//        return new RussianLuceneMorphology();
//    }
}
