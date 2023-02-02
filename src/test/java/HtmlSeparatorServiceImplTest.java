import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.Application;
import searchengine.model.Page;
import searchengine.model.Site;

@SpringBootTest(classes = {Application.class})
public class HtmlSeparatorServiceImplTest {

    @BeforeEach
    public void setup() {
        Site site = new Site();
        site.setName("Test");
    }
}
