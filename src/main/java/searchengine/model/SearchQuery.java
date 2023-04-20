package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class SearchQuery {

    String query;
    Integer offset;
    Integer limit;
    List<Site> sites;

    public SearchQuery (String query, Integer offset, Integer limit) {
        this.query = query;
        this.offset = offset;
        this.limit = limit;
        sites = new ArrayList<>();
    }
}