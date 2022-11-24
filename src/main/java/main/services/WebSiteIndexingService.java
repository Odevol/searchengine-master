package main.services;

import main.model.*;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class WebSiteIndexingService extends RecursiveAction {

    private String path;
    private Site site;
    private final RepositoryStorage repositoryStorage;
    private final static String regex = "[^a-zA-Zа-яА-ЯёЁ<!>\\s=\"%0-9:.\\/_,;\\(\\)\\{\\}\\[\\]|'&?+#№@—–‑«»$\\\\]+";
    private static boolean isRun;

    public WebSiteIndexingService(String path, Site site, RepositoryStorage repositoryStorage){
        this.path = path;
        this.site = site;
        this.repositoryStorage = repositoryStorage;
    }

    @Override
    protected void compute() {
        List<WebSiteIndexingService> taskList = new ArrayList<>();
        Document document = null;
        if(checkURL(site.getUrl(), path)){
            String replace = site.getUrl().replace("www.", "");
            path = path.replace(site.getUrl(), "").replace(replace, "");
        }
        String url = site.getUrl().concat(path);
        Page page;
        try {
            int code = getCode(url);
            if (code == 0) return;
            if (!(code == 200)) {
                page = new Page(path, code, "", site);
                new PageService(repositoryStorage.getPageRepository()).addPage(page);
                return;
            }
            document = getDocument(url);
            String content = document.toString().replace("'", "").replace("ё", "е")
                    .replace("Ё", "Е").replaceAll(regex, "");

            page = new Page((path.equals("") ? "/" : path), code, content, site);
            if (isRun) {
                synchronized (repositoryStorage.getPageRepository()) {
                    if (!(repositoryStorage.getPageRepository().getPageBySiteId(site).contains(page))) {
                        new PageService(repositoryStorage.getPageRepository()).addPage(page);
                        new SiteService(repositoryStorage.getSiteRepository()).timeUpdate(site);
                        new LemmaService(repositoryStorage.getLemmaRepository(), repositoryStorage.getIndexRepository()).addLemmaFromPage(page);
                    } else {
                        return;
                    }
                }
            }
            taskList.addAll(createTaskList(document));
            ForkJoinTask.invokeAll(taskList);
        } catch (Exception e) {
            site.setLastError(e.getClass().getName());
            e.printStackTrace();
            System.out.println(path);
        }
    }

    private static boolean checkURL(String url, String link){
        url = url.replace("www.", "");
        link = link.replace("www.", "");
        link.contains(url);
        return link.startsWith(url) && (link.length() > url.length());
    }

    private static boolean isContainPath(String path, PageRepository pageRepository, Site site){
        List<Page> pageList = pageRepository.getPageBySiteId(site);
        for(Page page : pageList){
            if(page.getPath().equalsIgnoreCase(path)){
                return true;
            }
        }
        return false;
    }
    public static boolean getIsRun() {
        return isRun;
    }

    public static void setIsRun(boolean isRun) {
        WebSiteIndexingService.isRun = isRun;
    }

    private Document getDocument(String url) throws InterruptedException {
        Document document = null;
        while (true){
            try {
                document = Jsoup.connect(url).maxBodySize(0).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US;" +
                                " rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .get();
                if (!document.equals(null)) break;
            } catch (ConnectException | NullPointerException | SSLHandshakeException e) {
                Thread.sleep(3000);
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return document;
    }

    private List<WebSiteIndexingService> createTaskList(Document document) throws InterruptedException {
        List<WebSiteIndexingService> taskList = new ArrayList<>();
        Elements elements = document.select("a[href]");
        for (Element element : elements){
            if (element.attr("href").equals("#")){
                continue;
            }
            String link = element.attr("href");
            String linkUrl = element.absUrl("href");
            if(checkURL(site.getUrl(), linkUrl) && !isContainPath(link, repositoryStorage.getPageRepository(), site)){
                Thread.sleep(150);
                WebSiteIndexingService task = new WebSiteIndexingService(link, site, repositoryStorage);
                taskList.add(task);
            }
        }
        return taskList;
    }

    private int getCode(String url) throws InterruptedException {
        int code = 0;
        while (code == 0){
            try {
                code = Jsoup.connect(url).ignoreHttpErrors(true).execute().statusCode();
            } catch (ConnectException | SocketTimeoutException e){
                Thread.sleep(3000);
                continue;
            } catch (UnsupportedMimeTypeException e){
                return 0;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return code;
    }
}
