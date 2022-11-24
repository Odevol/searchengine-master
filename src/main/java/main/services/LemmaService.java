package main.services;

import lombok.RequiredArgsConstructor;
import main.model.*;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LemmaService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private static final String[] regexWords = {"ПРЕДЛ", "СОЮЗ", "МЕЖД", "ЧАСТ"};
    private static LuceneMorphology luceneMorphology;

    static {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Lemma> list(){
        Iterable<Lemma> lemmaIterable = lemmaRepository.findAll();
        List<Lemma> lemmaList = new ArrayList<>();
        for(Lemma lemma : lemmaIterable){
            lemmaList.add(lemma);
        }
        return lemmaList;
    }

    public synchronized Lemma addNewLemma(String word, Site site){
        Lemma lemma = new Lemma();
        lemma.setLemma(word);
        lemma.setFrequency(1);
        lemma.setSite(site);
        return lemmaRepository.save(lemma);
    }

    public Lemma frequencyCount(String word){
        Lemma lemma = getLemmaByWord(word);
        lemma.setFrequency(lemma.getFrequency() + 1);
        return lemmaRepository.save(lemma);
    }

    public Lemma getLemmaByWord(String word){
        List<Lemma> lemmaList = list();
        for(Lemma lemma : lemmaList){
            if (lemma.getLemma().equalsIgnoreCase(word)){
                return lemma;
            }
        }
        return null;
    }

    public synchronized void addLemmaFromPage(Page page) {
        String text = Jsoup.parse(page.getContent()).text();
        List<Index> indexList = new ArrayList<>();
        try {
            HashMap<String, Integer> hashMap = count(text);
            Set<String> hashSet = hashMap.keySet();
            synchronized (lemmaRepository) {
                for (String key : hashSet) {
                    Lemma lemma;
                    if (isContain(key)) {
                        lemma = frequencyCount(key);
                    } else {
                        lemma = addNewLemma(key, page.getSite());
                    }
                    if(indexList.size() < 200){
                        indexList.add(new IndexService().addIndex(page, lemma, hashMap));
                    } else {
                        indexRepository.saveAll(indexList);
                        indexList.clear();
                    }
                }
                indexRepository.saveAll(indexList);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private boolean isContain(String key) {
        Iterable<Lemma> lemmaIterable = lemmaRepository.findAll();
        for (Lemma lemma : lemmaIterable){
            if (lemma.getLemma().equalsIgnoreCase(key)){
                return true;
            }
        }
        return false;
    }

    public HashMap<String,Integer> count(String text) throws IOException {
        String textRegex = "[^а-яА-Я]+";
        String[] words = text.split(textRegex);
        HashMap<String, Integer> textMap = new HashMap<>();
        for (String word : words){
            List<String> wordBaseForm = luceneMorphology.getNormalForms(word.toLowerCase(Locale.ROOT));
            if ((word.length() < 2) || isWord(luceneMorphology.getMorphInfo(wordBaseForm.get(0)).get(0))){
                continue;
            }
            if (!textMap.containsKey(wordBaseForm.get(0))){
                textMap.put(wordBaseForm.get(0), 1);
            } else {
                textMap.put(wordBaseForm.get(0), textMap.get(wordBaseForm.get(0)) + 1);
            }
        }
        return textMap;
    }

    private boolean isWord(String word) {
        for (String regex : regexWords){
            if (word.contains(regex)){
                return true;
            }
        }
        return false;
    }
}
