package main.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {

    @Query("SELECT l FROM Lemma l WHERE l.site=:site AND l.lemma IN :wordSet ORDER BY frequency ASC")
    Set<Lemma> getLemmaSetByWordList(@Param("site") Site site, @Param("wordSet") Set<String> wordSet);
}
