package searchengine.controllers;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SearchQuery;
import searchengine.services.StatisticsService;
import searchengine.services.indexService.IndexService;
import searchengine.services.searchService.SearchResult;
import searchengine.services.searchService.SearchService;

import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final SearchService searchService;
    private final IndexService indexService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() {
        return indexService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        return indexService.stopIndexing();
    }

    @GetMapping("/startSeparation")
    public ResponseEntity<String> startSeparation() {
        return indexService.startSeparation();
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResult> search(@RequestParam("query") String query, @RequestParam(value = "offset", required = false) Integer offset,
                                               @RequestParam(value = "limit", required = false) Integer limit,
                                               @RequestParam(value = "site", required = false) String site) {
        return searchService.search(query, offset, limit, site);
    }
}
