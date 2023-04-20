package searchengine.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;

@Entity
@Table(name = "index", schema = "search_engine")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "page_id", nullable = false)
    @ToString.Exclude
    private Page page;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    @ToString.Exclude
    private Lemma lemma;

    @Column(nullable = false)
    private Float rank;

    public Index(Page page, Lemma lemma, Float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Index index = (Index) o;
        return id != null && Objects.equals(id, index.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
