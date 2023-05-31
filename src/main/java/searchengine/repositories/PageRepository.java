package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

@Repository
@Transactional
public interface PageRepository extends CrudRepository<Page, Integer> {

    List<Page> findAllBySite(Site site);

    List<Page> findAllByContentIsContainingAndSite(String word, Site site);

    List<Page> findAllBySiteAndContentLike(Site site, String lemma);

    Integer countAllBySite(Site site);
}
