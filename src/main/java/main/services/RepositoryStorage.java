package main.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import main.model.IndexRepository;
import main.model.LemmaRepository;
import main.model.PageRepository;
import main.model.SiteRepository;
import org.springframework.stereotype.Service;

@Getter
@AllArgsConstructor
@Service
public class RepositoryStorage {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
}
