package searchengine.services.searchService;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.wordProcessorService.WordProcessorService;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {

    final SiteRepository siteRepository;
    final LemmaRepository lemmaRepository;
    final IndexRepository indexRepository;
    final WordProcessorService wordProcessorService;

    final PageRepository pageRepository;
    final LuceneMorphology luceneMorphology;
    public ResponseEntity<SearchResult> search(String query, Integer offset, Integer limit, String site) {
        SearchQuery searchQuery = new SearchQuery(query, offset, limit, new ArrayList<>());
        SearchResult searchResult = new SearchResult();

        if (site != null) {
            Site siteFromDB = siteRepository.findAllByUrl(site);
            searchQuery.getSites().add(siteFromDB);
        } else {
            siteRepository.findAll().forEach(e->{
                searchQuery.getSites().add(e);
            });
        }


        searchResult = getSearchResult(searchQuery);

        return new ResponseEntity<>(searchResult, HttpStatus.OK);
    }

    private SearchResult getSearchResult(SearchQuery searchQuery) {
        SearchResult searchResult = new SearchResult();
        if (searchQuery.getQuery().isEmpty()) {
            searchResult.setResult(false);
            searchResult.setError("Задан пустой поисковый запрос");
            return searchResult;
        } else {
//            List<Page> resultPages = getPages(searchQuery);
//            if (resultPages.size()!=0) {
//                //TODO написать метод поиска сниппетов
//                HashMap<Page, String> snippets = getSnippets(resultPages, searchQuery);
//                assert snippets != null;
//                searchResult.setResult(true);
//                searchResult.setError(null);
//                snippets.forEach((k, v) -> {
//                    searchResult.getDataList().add(new Data(k.getSite(), k.getSite().getName(), k.getPath(),
//                            new Document(k.getContent()).title(), v, 0d));
//                });
//            } else {
//                searchResult.setResult(false);
//                searchResult.setError("Не найдено страниц");
//            }


            //Тестовая заглушка
            searchResult.setResult(true);
            searchResult.setError(null);
            searchResult.setCount(10);
            ArrayList<Data> dataArrayList = new ArrayList<>();
            Site site = siteRepository.findAll().get(0);
            for (int i = 0; i<15; i++) {
                Data data = new Data();
                data.setRelevance(i+1.0);
                data.setSite(site);
                data.setTitle("Заглушка");
                data.setSnippet("Считаем количество строк для сниппета может одна может две может три может четыре," +
                        "может пять, может шесть, может семь, может восемь, может девять, может десять, может одиннадцать" +
                        ", может двенадцать, может тринадцать, может четырнадцать, может пятнадцать, может шестандцать, " +
                        "может семнадцать, может восемьнадцать, может девятнадцать, может двадцать");
                data.setSiteName(site.getName());
                data.setUrl("www.заглушка.com");
                dataArrayList.add(data);
            }
            searchResult.setData(dataArrayList);
            return searchResult;
        }
    }

    private HashMap<Page, String> getSnippets(List<Page> resultPages, SearchQuery searchQuery) {
        HashMap<Page, String> snippets = new HashMap<>();
        List<String> words = extractLemmas(searchQuery);
        for (Page page : resultPages) {
            String content = Jsoup.clean(page.getContent(), Safelist.simpleText()).toLowerCase();
            ArrayList<String> wordsFromPage = new ArrayList<>(Arrays.asList(content.split("\\s+")));
            //TODO вылетает при попытке нормализовать слова и символы англоязычные
            wordsFromPage.forEach(e -> {
                try {
                    if (wordProcessorService.ifWord(e)) {
                        luceneMorphology.getNormalForms(e).get(0);
                    }
                } catch (WrongCharaterException ignored) {

                }
            });

            List<String> stringsForSnippets = getStringForSnippets(wordsFromPage, words, content);
//            int firstWord = wordsFromPage.indexOf(words.get(0));
//            int lastWord = wordsFromPage.indexOf(words.get(words.size()-1));
//            Integer firstWordPosition=0;
//            Integer lastWordPosition = 0;
//            for (int i = 0; i<lastWord; i++) {
//                if (i<=firstWord) {
//                    firstWordPosition = content.indexOf(" ", firstWordPosition);
//                }
//                lastWordPosition = content.indexOf(" ", lastWordPosition);
//            }
            //TODO дописать метод проверяющий строки полученные для сниппетов на наличии слов из поискового запроса в данной строке
//            boolean stringContainsSnippet = false;
//            for (String string : stringsForSnippets) {
//                if (string.contains())
//            }
        }
        return null;
    }

    public List<Page> getPages(SearchQuery searchQuery) {
        List<Page> result = new ArrayList<>();
        List<Index> indexResult = new ArrayList<>();
        List<String> words = extractLemmas(searchQuery);
        for (Site site : searchQuery.getSites()) {
            //Получаем леммы из бд
            List<Lemma> lemmaList = getLemmasFromDB(words, site);

            if (lemmaList.size()!=0) {
                //Сортировка лемм по частоте
                sortLemmasByFrequency(lemmaList);
                //Получаем количество страниц на сайте, для сравнения на каком количестве страниц встречаются леммы
                // относительно всех страниц
                Integer pagesOnSite = pageRepository.countAllBySite(site);
                for (Lemma lemma : lemmaList) {
                    if (lemma!=null) {
                        List<Index> indexList = indexRepository.findAllByLemma_SiteAndLemma(site, lemma);
                        if (indexList.size()!=0 && indexList.size()<pagesOnSite/10) {
                            removeMismatches(indexResult, indexList);
                        }
                    }
                }
            }
        }
        for (Index index : indexResult) {
            result.add(index.getPage());
        }
        //TODO отсортировать старницы по релевантности
        return result;
    }


    // дописать метод чтобы возвращал список строк в которых потом искать совпадает ли они с поисковым запросом
    //(т.к. метод ищет подстроку по начальному слову и конечному
    public List<String> getStringForSnippets(ArrayList<String> wordsFromPage, List<String> words, String content) {
        List<String> result = new ArrayList<>();

        int firstWordPosition;
        int lastWordPosition;
        //TODO метод не работает зацикливается стартовый индекс не двигается
        do {
            //Получаем первую позицию слова в тексте со страницы
            int firstWord = wordsFromPage.indexOf(words.get(0));
            //Получаем первую позицию последнего слова в тексте со страницы
            int lastWord = wordsFromPage.indexOf(words.get(words.size() - 1));
            firstWordPosition = 0;
            lastWordPosition = 0;
            for (int i = 0; i < lastWord; i++) {
                if (i <= firstWord) {
                    firstWordPosition = content.indexOf(" ", firstWordPosition);
                }
                lastWordPosition = content.indexOf(" ", lastWordPosition);
                if (i == lastWord - 1) {
                    result.add(content.substring(firstWordPosition, lastWordPosition));
                    wordsFromPage.set(firstWord, null);
                    wordsFromPage.set(lastWord, null);
                }
            }
        } while (firstWordPosition >= 0 && lastWordPosition >= 0);

        return result;
    }

    private List<String> extractLemmas(SearchQuery searchQuery) {
        List<String> words = new ArrayList<>(Arrays.asList(searchQuery.getQuery().toLowerCase().split("\\s+")));
        removeNotWords(words);
        return words;
    }

    private void removeMismatches(List<Index> indexResult, List<Index> indexList) {
        if (indexResult.isEmpty()) {
            indexResult.addAll(indexList);
        } else {
            indexResult.forEach(e -> {
                if (!indexList.contains(e)) {
                    indexResult.remove(e);
                }
            });
        }
    }

    private List<Lemma> getLemmasFromDB(List<String> words, Site site) {
        ArrayList<Lemma> lemmata = new ArrayList<>();
        for (String word : words) {
            Lemma lemma = lemmaRepository.findLemmaBySiteIdAndLemma(site.getId(), luceneMorphology.getNormalForms(word).get(0));
            if (lemma!=null) {
                lemmata.add(lemma);
            }
        }
        return lemmata;
    }

    private void sortLemmasByFrequency(List<Lemma> lemmas) {
        Collections.sort(lemmas);
    }

    private void removeNotWords(List<String> words) {
        words.removeIf(word -> !wordProcessorService.ifWord(word));
    }
}
