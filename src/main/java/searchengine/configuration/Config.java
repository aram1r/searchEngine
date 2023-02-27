package searchengine.configuration;


import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import searchengine.services.indexService.taskPools.TaskPool;

import java.io.IOException;

@Configuration
public class Config {
    @Bean
    public TaskPool taskPool () {
        return new TaskPool();
    }

    @Bean
    public AppProps appProps() {
        return new AppProps();
    }

//    @Bean
//    public LuceneMorphology luceneMorphology() throws IOException {
//        return new RussianLuceneMorphology();
//    }
    @Bean
    public RussianLuceneMorphology luceneMorphology() throws IOException {
        return new RussianLuceneMorphology();
    }
}
