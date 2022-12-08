package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "page", schema = "search_engine", indexes = @Index(columnList = "path"))
@Data
@NoArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column
    private String path;

    @Column(nullable = false)
    private Integer responseCode;

    @Column
    @Lob
    private String content;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
    @JoinColumn(name = "site_id", insertable = false, updatable = false)
    private Site site;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return path.equals(page.path) && site.equals(page.site);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, site);
    }
}
