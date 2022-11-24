package main.services;

import main.model.Site;
import main.model.SiteRepository;
import main.model.SiteStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SiteService {

    private final SiteRepository siteRepository;

    public SiteService(SiteRepository siteRepository){
        this.siteRepository = siteRepository;
    }

    public List<Site> list(){
        Iterable<Site> siteIterable = siteRepository.findAll();
        List<Site> siteList = new ArrayList<>();
        for(Site site : siteIterable){
            siteList.add(site);
        }
        return siteList;
    }

    public void addSite(Site site){
        siteRepository.save(site);
    }

    public Site getById(Integer id){
        return siteRepository.findById(id).get();
    }

    public Site getByURL(String url){
        Iterable<Site> siteIterable = siteRepository.findAll();
        for(Site site : siteIterable){
            if(site.getUrl().equalsIgnoreCase(url)){
                return site;
            }
        }
        Site site = new Site();
        site.setUrl("");
        return site;
    }

    public void delete(Site site){
        siteRepository.delete(site);
    }

    public void deleteById(Integer id){
        siteRepository.deleteById(id);
    }

    public void timeUpdate(Site site) {
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    public void statusUpdate(Site site, SiteStatus siteStatus) {
        site.setStatus(siteStatus);
        siteRepository.save(site);
    }
}
