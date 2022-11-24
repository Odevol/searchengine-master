package main.services;

import main.dto.indexing.IndexingResponse;
import main.model.Site;

public interface IndexingService {

    void startSiteIndexing(Site site);

    boolean isContainInDB(String url);

    Site addNewSite(Site site);

    Site clearSite(Site site);

    void stopIndexing();

    IndexingResponse indexPage(String url);
}
