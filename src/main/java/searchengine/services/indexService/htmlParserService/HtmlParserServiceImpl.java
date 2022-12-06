package searchengine.services.indexService.htmlParserService;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import searchengine.model.Site;

import java.util.concurrent.RecursiveAction;

@Service
@Getter
@Setter
@NoArgsConstructor
public class HtmlParserServiceImpl extends RecursiveAction implements HtmlParserService{

    private Site site;

    @Override
    protected void compute() {

    }
}
