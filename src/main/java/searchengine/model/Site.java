package searchengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;


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

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "site", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Page> pageList;

    public Site(Status status, LocalDateTime statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Site{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
