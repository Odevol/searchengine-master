package main.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {

    @Query("SELECT p FROM Page p WHERE p.site=:site")
    List<Page> getPageBySiteId(@Param("site") Site site);
}
