package main.controllers;

import lombok.RequiredArgsConstructor;
import main.config.SitesList;
import main.dto.indexing.IndexingResponse;
import main.dto.search.SearchResponse;
import main.model.*;
import main.services.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import main.dto.statistics.StatisticsResponse;

import java.util.ArrayList;;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing(){
        System.out.println("Start");
        if (!WebSiteIndexingService.getIsRun()) {
            Thread thread = new Thread(new IndexingServiceImpl(new RepositoryStorage(siteRepository,
                    pageRepository, lemmaRepository, indexRepository), sitesList));
            thread.setDaemon(true);
            thread.start();
            System.out.println("end");
            return ResponseEntity.ok(new IndexingResponseService().getTrueResponse());
        } else {
            return ResponseEntity.ok(new IndexingResponseService().getFalseResponse("Indexing is run"));
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing(){
        if (WebSiteIndexingService.getIsRun()){
            WebSiteIndexingService.setIsRun(false);
            return ResponseEntity.ok(new IndexingResponseService().getTrueResponse());
        }else {
            return ResponseEntity.ok(new IndexingResponseService().getFalseResponse("Indexing is not run"));
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url){
        return ResponseEntity.ok(
                new IndexingServiceImpl(new RepositoryStorage(siteRepository,
                        pageRepository, lemmaRepository, indexRepository), null)
                        .indexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query, @RequestParam(defaultValue = "") String site){
        List<Site> sites;
        if(site.isEmpty()){
            sites = new SiteService(siteRepository).list();
        }
        else {
            sites = new ArrayList<>();
            sites.add(new SiteService(siteRepository).getByURL(site));
        }
        SearchResponse searchResponse = new SearchServiceImpl(new RepositoryStorage(siteRepository
                ,pageRepository, lemmaRepository, indexRepository)).getSearchResponse(query, sites);
        return ResponseEntity.ok(searchResponse);
    }
}
