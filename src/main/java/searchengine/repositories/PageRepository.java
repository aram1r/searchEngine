package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

@Repository
@Transactional
public interface PageRepository extends CrudRepository<Page, Integer> {

    public List<Page> findAllBySite(Site site);
}
