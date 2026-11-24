package cl.transbank.webpay.example.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NavigationMenuTests {

    private static final String TRANSACCION_COMPLETA_LINK = "href=\"/transaccion-completa\"";
    private static final List<String> HIDDEN_TRANSACCION_COMPLETA_LINKS = List.of(
            "href=\"/transaccion-completa-diferida",
            "href=\"/transaccion-completa-mall");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void desktopSidebarShowsTransaccionCompleta() throws Exception {
        String sidebar = asideById(render("/transaccion-completa"), "sidebar-desk");

        assertThat(sidebar).contains("Transacción Completa").contains(TRANSACCION_COMPLETA_LINK);
    }

    @Test
    void mobileSidebarShowsTransaccionCompleta() throws Exception {
        String sidebar = asideById(render("/"), "sidebar");

        assertThat(sidebar).contains("Transacción Completa").contains(TRANSACCION_COMPLETA_LINK);
    }

    @Test
    void homeShowsTransaccionCompletaCard() throws Exception {
        String home = render("/");

        assertThat(cardLinks(home)).contains("/transaccion-completa");
    }

    @Test
    void sidebarsDoNotExposeOtherTransaccionCompletaModalities() throws Exception {
        String page = render("/transaccion-completa");

        for (String sidebarId : List.of("sidebar-desk", "sidebar")) {
            String sidebar = asideById(page, sidebarId);
            HIDDEN_TRANSACCION_COMPLETA_LINKS.forEach(link -> assertThat(sidebar).doesNotContain(link));
        }
    }

    private String render(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static String asideById(String html, String id) {
        int idIndex = html.indexOf("id=\"" + id + "\"");
        assertThat(idIndex).as("aside with id %s", id).isNotNegative();
        int asideStart = html.lastIndexOf("<aside", idIndex);
        int contentStart = html.indexOf('>', idIndex) + 1;
        int contentEnd = html.indexOf("</aside>", contentStart);
        assertThat(asideStart).as("opening aside for id %s", id).isNotNegative();
        assertThat(contentEnd).as("closing aside for id %s", id).isNotNegative();
        return html.substring(contentStart, contentEnd);
    }

    private static List<String> cardLinks(String html) {
        List<String> links = new ArrayList<>();
        int anchorStart = html.indexOf("<a ");
        while (anchorStart >= 0) {
            int anchorEnd = html.indexOf('>', anchorStart);
            String anchor = html.substring(anchorStart, anchorEnd);
            if (anchor.contains("class=\"card-info-link\"")) {
                links.add(attribute(anchor, "href"));
            }
            anchorStart = html.indexOf("<a ", anchorEnd);
        }
        return links;
    }

    private static String attribute(String tag, String name) {
        String prefix = name + "=\"";
        int valueStart = tag.indexOf(prefix) + prefix.length();
        return tag.substring(valueStart, tag.indexOf('"', valueStart));
    }
}
