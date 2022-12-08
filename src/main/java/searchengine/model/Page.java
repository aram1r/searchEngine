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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String path;

    @Column(nullable = false)
    private Integer responseCode;

    @Column
    @Lob
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    public Page(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return path.equals(page.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
