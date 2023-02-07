package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;

@Entity
@Table(name = "lemma", schema = "search_engine")
@Data
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private Integer frequency;

    public Lemma (String lemma, Integer frequency, Site site) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.site = site;
    }
}
