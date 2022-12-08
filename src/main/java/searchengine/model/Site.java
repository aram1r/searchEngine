package searchengine.model;

import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name="sites", schema = "search_engine")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    @Enumerated(EnumType.STRING)
    @Type(type = "searchengine.model.Status")
    private Status status;

    @Column(name="status_time")
    private LocalDateTime statusTime;

    @Column(name = "last_error")
    @ColumnDefault("null")
    private String lastError;

    @Column(name="url", nullable = false, unique = true)
    private String url;

    @Column(name="name", nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "site")
    private List<Page> pageList;

    public Site(Status status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }
}
