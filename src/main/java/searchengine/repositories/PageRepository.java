package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

@Repository
@Transactional
public interface PageRepository extends CrudRepository<Page, Integer> {
}
