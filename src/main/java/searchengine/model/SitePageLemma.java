package searchengine.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@NoArgsConstructor
@Data
public class SitePageLemma {

    @EmbeddedId
    SitePageLemmaID id;

    @Column(name = "rank")
    private Float rank;

    @Column(name = "word")
    private String word;

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "lemma_id", nullable = false)
//    private Lemma lemma;

}

@Embeddable
class SitePageLemmaID implements Serializable {

    @OneToOne(fetch = FetchType.LAZY)
    Site site;

    @OneToOne(fetch = FetchType.LAZY)
    Page page;

    @OneToOne(fetch = FetchType.LAZY)
    Lemma lemma;
}
