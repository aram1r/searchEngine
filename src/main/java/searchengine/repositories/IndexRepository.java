package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;

@Repository
@Transactional
public interface IndexRepository extends CrudRepository<Index, Integer> {
}
