package services.searchServiceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;
import searchengine.services.searchService.SearchService;

import java.time.LocalDateTime;
import java.util.ArrayList;

@SpringBootTest(classes = {Application.class, Page.class, Site.class, Lemma.class})
@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @InjectMocks
    SearchService searchService;

    @Mock
    SiteRepository siteRepository;

    Site site;

    @Autowired
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @BeforeEach
    public void setup() {
        site = new Site();
        site.setName("test.com");
        site.setStatus(Status.INDEXED);
        site.setUrl("test.com");
        site.setStatusTime(LocalDateTime.now());
        site.setPageList(new ArrayList<>());
    }

    @Test
    void searchTest() {
        Mockito.when(siteRepository.findAllByUrl("site")).thenReturn(site);
        searchService.search("Поисковый запрос", 2, 15, site.getName());

    }


}
