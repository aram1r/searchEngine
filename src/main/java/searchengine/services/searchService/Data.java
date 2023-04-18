package searchengine.services.searchService;

import lombok.*;
import searchengine.model.Site;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Data {
    private Site site;
    private String siteName;
    private String url;
    private String title;
    private String snippet;
    private Double relevance;
}
