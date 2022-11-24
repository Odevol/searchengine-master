package main.services;

import main.model.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class IndexService{

    public Index addIndex(Page page, Lemma lemma, HashMap<String, Integer> count){
        Index index = new Index();
        index.setPageId(page.getId());
        index.setLemmaId(lemma.getId());
        index.setRank(Float.valueOf(count.get(lemma.getLemma())));
        return index;
    }

//    private Float calculateRank(Page page, Lemma lemma) {
//        Float calcRank = 0f;
//        Document doc = Jsoup.parse(page.getContent());
//        String word = lemma.getLemma();
//        try {
//            Map<String, Integer> titleLemmaCount = new LemmaService(lemmaRepository, indexRepository).count(doc.select("title").get(0).toString());
//            Map<String, Integer> bodyLemmaCount = new LemmaService(lemmaRepository, indexRepository).count(doc.select("body").toString());
//            Float titleRank = Float.valueOf(titleLemmaCount.containsKey(word) ? titleLemmaCount.get(word) : 0);
//            Float bodyRank = Float.valueOf(bodyLemmaCount.containsKey(word) ? (bodyLemmaCount.get(word)) * 0.8F : 0);
//            calcRank = titleRank + bodyRank;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return calcRank;
//    }
}
