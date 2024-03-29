package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;
import java.util.List;

@Repository
@Transactional
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {

    List<Lemma> findAllBySite(Site site);

    void deleteAllBySite(Site site);

    Lemma findLemmaByLemma(String string);

    Lemma findLemmaBySiteIdAndLemma(Integer siteId, String string);


}
