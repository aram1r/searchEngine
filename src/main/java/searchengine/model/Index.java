package searchengine.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "index", schema = "search_engine")
@Data
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(nullable = false)
    private Float rank;
}
