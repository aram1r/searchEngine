package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;

import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends CrudRepository<Index, Integer> {
    List<Index> findAllByLemmaId(Integer id);

    @Query(value = "SELECT search_engine.index.* \n" +
            "FROM search_engine.lemma INNER JOIN search_engine.index on search_engine.lemma.id = search_engine.index.lemma_id\n" +
            "INNER JOIN search_engine.page on search_engine.index.page_id = search_engine.page.id\n" +
            "WHERE search_engine.page.site_id= :siteID and search_engine.lemma.id= :lemmaID", nativeQuery = true)
    List<Index> findAllByLemmaIdAndSiteId(@Param("siteID") Integer siteID, @Param("lemmaID")Integer lemmaId);
}
