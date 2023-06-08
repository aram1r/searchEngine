package searchengine.services.searchService;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SearchResult {
    private boolean result;
    private String error;
    private Integer count;
    private List<Data> dataList;

    public SearchResult() {
        dataList = new ArrayList<>();
    }
}
