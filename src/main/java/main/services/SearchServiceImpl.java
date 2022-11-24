package main.services;

import lombok.AllArgsConstructor;
import main.dto.search.SearchData;
import main.dto.search.SearchResponse;
import main.model.*;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@AllArgsConstructor
public class SearchServiceImpl implements SearchService{

    private final RepositoryStorage repositoryStorage;

    @Override
    public SearchResponse getSearchResponse(String query, List<Site> sitesList){
        SearchResponse searchResponse = new SearchResponse();
        if(query.isEmpty()) return searchResponse;
        Map<Page, Float> pageFloatMap = new HashMap<>();
        sitesList.parallelStream().forEach(site -> {
            pageFloatMap.putAll(search(query, site));
        });
        searchResponse.setResult(true);
        searchResponse.setCount(pageFloatMap.size());
        List<SearchData> searchDataList = new ArrayList<>();
        Set<String> querySet = null;
        try {
            querySet = new LemmaService(repositoryStorage.getLemmaRepository(), repositoryStorage.getIndexRepository()).count(query).keySet();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Set<String> finalQuerySet = querySet;
        pageFloatMap.keySet().parallelStream().forEach(page -> {
            searchDataList.add(generateSearchData(page, pageFloatMap.get(page), finalQuerySet));
        });
        List<SearchData> sortedSearchDataList = searchDataList.stream().sorted(
                Comparator.comparing(SearchData::getRelevance).reversed()).collect(Collectors.toList());
        searchResponse.setData(sortedSearchDataList);
        return searchResponse;
    }

    private SearchData generateSearchData(Page page, Float relevance, Set<String> querySet){
        Site site = page.getSite();
        SearchData searchData = new SearchData();
        searchData.setSite(site.getUrl());
        searchData.setSiteName(site.getName());
        searchData.setUri(page.getPath());
        Document doc = Jsoup.parse(page.getContent());
        String title = doc.select("title").text();
        searchData.setTitle(title);
        try {
            searchData.setSnippet(setSnippet(doc, querySet));
        } catch (IOException e) {
            e.printStackTrace();
        }
        searchData.setRelevance(relevance);
        return searchData;
    }

    private String setSnippet(Document doc, Set<String> querySet) throws IOException {
        Element element = doc.select("html").get(0);
        List<Element> elementList = findAllChild(element);
        StringBuilder blocks = new StringBuilder("");
        Set<String> textBlocks = new HashSet<>();
        elementList.forEach(e -> {
            try {
                String text = e.text();
                boolean isContain = isContainRequest(e, querySet);
                if(!text.isEmpty() && isContain){
                    textBlocks.add(text);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        textBlocks.forEach(t -> {
            try {
                String withBold = bolding(t, querySet);
                blocks.append("<p>").append(withBold).append("</p>\n\r");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        String snippet = blocks.toString();
        return snippet;
    }

    private String bolding(String text, Set<String> querySet) throws IOException {
        String textRegex = "[^а-яА-ЯёЁ]+";
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        String[] words = text.split(textRegex);
        Set<String> wordSet = new HashSet<>();
        wordSet.addAll(List.of(words));
        AtomicReference<String> newText = new AtomicReference<>();
        newText.set(text);
        wordSet.forEach(w->{
            if(!(w.length() > 0)){
                return;
            }
            String wordBaseForm = luceneMorphology.getNormalForms(w.toLowerCase(Locale.ROOT)).get(0);
            if(querySet.contains(wordBaseForm)){
                String bold = "<b>" + w + "</b>";
                newText.set(newText.get().replace(w, bold));
            }
        });
        return newText.get();
    }

    private boolean isContainRequest(Element element, Set<String> querySet) throws IOException {
        List<String> blockLemmas = new LemmaService(repositoryStorage.getLemmaRepository(), repositoryStorage.getIndexRepository()).count(element.text()).keySet().stream().toList();
        List<String> stringList = querySet.stream().toList();
        for (int i = 0; i < blockLemmas.size(); i++){
            if (stringList.contains(blockLemmas.get(i))){
                return true;
            }
        }
        return false;
    }

    private List<Element> findAllChild(Element element) {
        Elements elements = element.children();
        List<Element> elementList = new ArrayList<>();
        elements.forEach(e -> {
            if(e.children().isEmpty()){
                elementList.add(e);
            } else {
                elementList.addAll(findAllChild(e));
            }
        });
        return elementList;
    }

    private Map<Page, Float> search(String query, Site site) {
        List<Page> pageList = new ArrayList<>();
        List<Lemma> lemmaList = new ArrayList<>();
        try {
            Set<String> querySet = new LemmaService(repositoryStorage.getLemmaRepository(), repositoryStorage.getIndexRepository()).count(query).keySet();
            lemmaList.addAll(repositoryStorage.getLemmaRepository().getLemmaSetByWordList(site, querySet).stream().toList());

            List<Index> indexList = repositoryStorage.getIndexRepository().getIndexByLemmaId(lemmaList.get(0).getId());

            indexList.forEach(index -> {
                pageList.add(repositoryStorage.getPageRepository().findById(index.getPageId()).get());
            });
            for(Lemma lemma : lemmaList){
                screening(lemma, pageList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(pageList.isEmpty()){
            return new HashMap<>();
        }
        Map<Page, Float> relevancePage = calculateRelevance(pageList, lemmaList);
        return relevancePage;
    }

    private Map<Page, Float> calculateRelevance(List<Page> pageList, List<Lemma> lemmaList) {
        Map<Page, Float> pageAbsRelevance = new HashMap<>();
        Map<Page, Float> pageRelRelevance = new HashMap<>();
        for (Page page : pageList){
            Float absRel = relevanceForPage(page, lemmaList);
            pageAbsRelevance.put(page, absRel);
        }

        Float maxAbsRelevance = pageAbsRelevance.values().stream().max(Comparator.naturalOrder()).get();

        Float finalMaxAbsRelevance = maxAbsRelevance;
        pageAbsRelevance.forEach((page, rel) ->{
            Float relRelevance = rel / finalMaxAbsRelevance;
            pageRelRelevance.put(page, relRelevance);
        });
        return pageRelRelevance;
    }

    private Float relevanceForPage(Page page, List<Lemma> lemmaList) {
        Float absRel = 0f;
        for (Lemma lemma : lemmaList){
            absRel = absRel + repositoryStorage.getIndexRepository().getIndexByAllId(page.getId(), lemma.getId()).getRank();
        }
        return absRel;
    }

    private void screening(Lemma lemma, List<Page> pageList) {
        List<Index> indexList = repositoryStorage.getIndexRepository().getIndexByLemmaId(lemma.getId());
        List<Page> newPageList = new ArrayList<>();
        List<Page> pageForDelete = new ArrayList<>();
        indexList.forEach(index -> {
            newPageList.add(repositoryStorage.getPageRepository().findById(index.getPageId()).get());
        });
        pageList.forEach(page -> {
            if(!newPageList.contains(page)){
                pageForDelete.add(page);
            }
        });
        pageForDelete.forEach(page -> pageList.remove(page));
    }
}
