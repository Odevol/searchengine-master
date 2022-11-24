package main.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IndexRepository extends CrudRepository<Index, Integer> {

    @Query("SELECT i FROM Index i WHERE i.lemmaId=:lemmaId")
    List<Index> getIndexByLemmaId(@Param("lemmaId") Integer lemmaId);

    @Query("SELECT i FROM Index i WHERE i.pageId=:pageId AND i.lemmaId=:lemmaId")
    Index getIndexByAllId(@Param("pageId") Integer pageId, @Param("lemmaId") Integer lemmaId);

    @Query("SELECT i FROM Index i WHERE i.pageId=:pageId")
    List<Index> getByPageId(@Param("pageId") Integer pageId);
}
