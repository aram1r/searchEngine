package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

@Repository
@Transactional
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
}
