package searchengine.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lemma", schema = "search_engine")
@NoArgsConstructor
@Data
public class Lemma implements Comparable<Lemma> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
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

    @Override
    public int compareTo(Lemma o) {
        return this.frequency - o.getFrequency();
    }
}
