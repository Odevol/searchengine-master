package main.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Objects;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"path", "site_id"}))
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT",nullable = false)
    private String content;
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    public Page(){}

    public Page(String path, int code, String content, Site site){
        this.path = path;
        this.code = code;
        this.content = content;
        this.site = site;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return path.equals(page.path) && site.equals(page.getSite());
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, site);
    }
}
