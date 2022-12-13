import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.config.AppProps;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;

@SpringBootTest(classes = {Application.class, Page.class, Site.class})
public class HtmlParserServiceImplTest {
    private AppProps appProps;

    @Autowired
    public void setAppProps(AppProps appProps) {
        this.appProps = appProps;
    }

    @Test
    public void getLinks() {
        Elements links = new Elements();
        try {
            Connection.Response response = Jsoup.connect("https://www.playback.ru").userAgent(appProps.getUserAgent())
                    .referrer(appProps.getReferrer()).execute();
            Document document = response.parse();
            links = document.select("[href]");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        assert links.size()>0;
    }
}
