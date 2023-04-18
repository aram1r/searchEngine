package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import searchengine.model.SitePageLemma;

import java.util.List;

public interface SitePageLemmaRepository extends CrudRepository<SitePageLemma, Integer> {

    //    @Query("select NEW searchengine.model.LemmaPage(searchengine.model.Lemma.lemma as word, searchengine.model.Lemma.id as lemma_id," +
//            "searchengine.model.Index.rank as rank, searchengine.model.Page.id as page_id, searchengine.model.Site.id as site_id)" +
//            "FROM searchengine.model.Lemma INNER join searchengine.model.Index on searchengine.model.Lemma.id = searchengine.model.Index.lemma_id" +
//            "INNER join  searchengine.model.Page on searchengine.model.Index.page_id = searchengine.model.Page.id " +
//            "where searchengine.model.Page.site_id = :siteID")
    @Query(value = "SELECT search_engine.lemma.lemma as word, search_engine.lemma.id as lemma_id, \n" +
            "search_engine.index.rank, search_engine.index.page_id, search_engine.page.site_id\n" +
            "FROM search_engine.lemma\n" +
            "INNER JOIN search_engine.index on search_engine.lemma.id = search_engine.index.lemma_id\n" +
            "INNER JOIN search_engine.page on search_engine.index.page_id = search_engine.page.id\n" +
            "WHERE search_engine.page.site_id= :siteID", nativeQuery = true)
    List<SitePageLemma> findAllBySiteID(@Param("siteID") Integer siteID);
}
