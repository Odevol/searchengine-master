package main.services;

import lombok.RequiredArgsConstructor;
import main.model.Page;
import main.model.PageRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;

    public Page addPage(Page page){
        pageRepository.save(page);
        return page;
    }

    public List<Page> list(){
        Iterable<Page> pageIterable = pageRepository.findAll();
        List<Page> pageList = new ArrayList<>();
        for(Page page : pageIterable){
            pageList.add(page);
        }
        return pageList;
    }

    public Page getPageByUrl(String url){
        for(Page page : pageRepository.findAll()){
            if((page.getSite().getUrl().concat(page.getPath())).equalsIgnoreCase(url)){
                return page;
            }
        }
        return new Page();
    }
}
