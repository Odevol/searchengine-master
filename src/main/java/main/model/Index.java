package main.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "`index`", uniqueConstraints = @UniqueConstraint(columnNames = {"page_id", "lemma_id"}))
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "page_id", nullable = false)
    private Integer pageId;
    @Column(name = "lemma_id", nullable = false)
    private Integer lemmaId;
    @Column(name = "`rank`", nullable = false)
    private Float rank;
}
