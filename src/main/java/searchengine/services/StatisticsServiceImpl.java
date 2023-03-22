package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.configuration.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private final SitesList sites;

    private SiteRepository siteRepository;
    private PageRepository pageRepository;

    private LemmaRepository lemmaRepository;

    @Autowired
    public void setLemmaRepository(LemmaRepository lemmaRepository) {this.lemmaRepository = lemmaRepository;}

    @Autowired
    public void setPageRepository(PageRepository pageRepository) {this.pageRepository = pageRepository;}

    @Autowired
    public void setSiteRepository(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        List<Site> siteList = siteRepository.findAll();
        total.setSites(siteList.size());
        total.setIndexing(true);
        int totalPages = 0;
        int totalLemmas = 0;

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for(Site site : siteList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = pageRepository.findAllBySite(site).size();
            int lemmas = lemmaRepository.findAllBySite(site).size();
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus().toString());
            item.setError(site.getLastError());
            LocalDateTime status = site.getStatusTime();
            item.setStatusTime(status.toString());
            detailed.add(item);

            totalPages+=pages;
            totalLemmas+=lemmas;
        }
        total.setPages(totalPages);
        total.setLemmas(totalLemmas);
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
