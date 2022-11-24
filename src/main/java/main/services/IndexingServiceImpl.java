package main.services;

import lombok.AllArgsConstructor;
import main.config.SitesList;
import main.dto.indexing.IndexingResponse;
import main.model.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@AllArgsConstructor
public class IndexingServiceImpl extends Thread implements IndexingService {

    private final RepositoryStorage repositoryStorage;
    private SitesList sitesList;

    public void run(){
        sitesList.getSites().parallelStream().forEach(site -> {
            startSiteIndexing(site);
        });
        WebSiteIndexingService.setIsRun(false);
    }

    @Override
    public void startSiteIndexing(Site site) {
        WebSiteIndexingService.setIsRun(true);
        Site indexingSite;
        if (isContainInDB(site.getUrl())) {
            indexingSite = clearSite(new SiteService(repositoryStorage.getSiteRepository()).getByURL(site.getUrl()));
        } else {
            indexingSite = addNewSite(site);
        }
        try {
            indexingSite.setLastError("");
            new SiteService(repositoryStorage.getSiteRepository()).statusUpdate(indexingSite, SiteStatus.INDEXING);
            new ForkJoinPool().invoke(new WebSiteIndexingService("", indexingSite, repositoryStorage));
            new SiteService(repositoryStorage.getSiteRepository()).statusUpdate(indexingSite,
                    (indexingSite.getLastError().isEmpty() ? SiteStatus.INDEXED : SiteStatus.FAILED));
        } catch (Exception e){
            indexingSite.setLastError(e.getClass().getName());
            indexingSite.setStatus(SiteStatus.FAILED);
            repositoryStorage.getSiteRepository().save(indexingSite);
        }
    }

    @Override
    public boolean isContainInDB(String url) {
        Site site = new Site();
        site.setUrl("");
        return !(new SiteService(repositoryStorage.getSiteRepository()).getByURL(url).equals(site));
    }

    @Override
    public Site addNewSite(Site site) {
        Site newSite = new Site();
        newSite.setStatus(SiteStatus.INDEXING);
        newSite.setUrl(site.getUrl());
        newSite.setName(site.getName());
        newSite.setStatusTime(LocalDateTime.now());
        new SiteService(repositoryStorage.getSiteRepository()).addSite(newSite);
        return newSite;
    }

    @Override
    public Site clearSite(Site site) {
        Site clearSite = new SiteService(repositoryStorage.getSiteRepository()).getByURL(site.getUrl());
        for(Page page : new PageService(repositoryStorage.getPageRepository()).list()){
            if(page.getSite().getId() == site.getId()){
                repositoryStorage.getPageRepository().delete(page);
            }
        }
        for(Lemma lemma : new LemmaService(repositoryStorage.getLemmaRepository(), repositoryStorage.getIndexRepository()).list()){
            if(lemma.getSite().getId() == site.getId()){
                repositoryStorage.getLemmaRepository().delete(lemma);
            }
        }
        return clearSite;
    }

    @Override
    public void stopIndexing() {
        WebSiteIndexingService.setIsRun(false);
    }

    @Override
    public IndexingResponse indexPage(String url) {
        Site pageSite = null;
        boolean startWithUrl = false;
        for (Site site : repositoryStorage.getSiteRepository().findAll()){
            if (url.startsWith(site.getUrl())){
                pageSite = site;
                startWithUrl = true;
                break;
            }
        }
        if (!startWithUrl){
            return new IndexingResponseService().getFalseResponse("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }
        Page page = new PageService(repositoryStorage.getPageRepository()).getPageByUrl(url);
        if(!page.equals(new Page())){
            clearPage(page);
        } else {
            page.setPath(url.substring(pageSite.getUrl().length()));
            page.setSite(pageSite);
        }
        try {
            page.setContent(Jsoup.connect(url).maxBodySize(0).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US;" +
                            " rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get().toString());
            page.setCode(Jsoup.connect(url).ignoreHttpErrors(true).execute().statusCode());
        } catch (IOException e) {
            page.setContent("");
        }
        repositoryStorage.getPageRepository().save(page);
        new LemmaService(repositoryStorage.getLemmaRepository(), repositoryStorage.getIndexRepository()).addLemmaFromPage(page);
        return new IndexingResponseService().getTrueResponse();
    }

    private void clearPage(Page page) {
        page.setContent("");
        repositoryStorage.getPageRepository().save(page);
        List<Index> indexForDelete = new ArrayList<>();
        Iterable<Index> indexIterable = repositoryStorage.getIndexRepository().getByPageId(page.getId());
        for(Index index : indexIterable){
            int pageId = index.getPageId();
            if(pageId == page.getId()){
                Lemma lemma = repositoryStorage.getLemmaRepository().findById(index.getLemmaId()).get();
                lemma.setFrequency(lemma.getFrequency() - 1);
                repositoryStorage.getLemmaRepository().save(lemma);
                indexForDelete.add(index);
            }
        }
        repositoryStorage.getIndexRepository().deleteAll(indexForDelete);
    }
}
