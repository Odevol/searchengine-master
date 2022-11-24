package main.services;

import main.dto.search.SearchResponse;
import main.model.Site;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    SearchResponse getSearchResponse(String query, List<Site> sitesList) throws IOException;
}
