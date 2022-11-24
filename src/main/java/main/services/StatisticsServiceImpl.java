package main.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import main.model.Site;
import main.dto.statistics.DetailedStatisticsItem;
import main.dto.statistics.StatisticsData;
import main.dto.statistics.StatisticsResponse;
import main.dto.statistics.TotalStatistics;

import java.util.ArrayList;
import java.util.List;
;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final RepositoryStorage repositoryStorage;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites((int) repositoryStorage.getSiteRepository().count());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for(Site site : new SiteService(repositoryStorage.getSiteRepository()).list()) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = site.getSetPage().size();
            int lemmas = site.getLemmaList().size();
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus().toString());
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime());


            detailed.add(item);
        }
        total.setLemmas((int) repositoryStorage.getLemmaRepository().count());
        total.setPages((int) repositoryStorage.getPageRepository().count());
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
