package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.util.List;


@Repository
@Transactional
public interface SiteRepository extends CrudRepository<Site, Integer> {
    void deleteAllByUrl(String url);

    void deleteByUrl(String url);

    Site getSiteByUrl(String url);

    void deleteAll();

    @Override
    List<Site> findAll();

    Site findAllByUrl(String name);
}
