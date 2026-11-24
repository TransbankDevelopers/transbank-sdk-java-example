package cl.transbank.webpay.example.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Matcher matcher = Pattern.compile("<aside[^>]*id=\"" + Pattern.quote(id) + "\"[^>]*>(.*?)</aside>", Pattern.DOTALL)
                .matcher(html);
        assertThat(matcher.find()).as("aside with id %s", id).isTrue();
        return matcher.group(1);
    }

    private static List<String> cardLinks(String html) {
        return Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*class=\"card-info-link\"")
                .matcher(html)
                .results()
                .map(result -> result.group(1))
                .toList();
    }
}
