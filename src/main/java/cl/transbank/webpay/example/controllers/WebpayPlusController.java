package cl.transbank.webpay.example.controllers;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.LinkedHashMap;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/webpay_plus")
public class WebpayPlusController extends BaseController {

    private WebpayPlus.Transaction tx;

    public WebpayPlusController() {
        tx = new WebpayPlus.Transaction(new WebpayOptions(IntegrationCommerceCodes.WEBPAY_PLUS,
                IntegrationApiKeys.WEBPAY, IntegrationType.TEST));
    }

    @GetMapping("/create")
    public String createPage(HttpServletRequest req, Model model) {
        Map<String, String> navigation = new LinkedHashMap<>() {
            {
                put("request", "Petición");
                put("response", "Respuesta");
                put("form", "Formulario");
            }
        };
        model.addAttribute("navigation", navigation);

        Map<String, String> breadcrumbs = new LinkedHashMap<>() {
            {
                put("Inicio", "/");
                put("Webpay Plus", "/webpay-plus/");
            }
        };
        model.addAttribute("breadcrumbs", breadcrumbs);

        String buyOrder = "buyOrder_" + getRandomNumber();
        String sessionId = "sessionId_" + getRandomNumber();
        int amount = 1000;
        String returnUrl = req.getRequestURL().toString().replace("create", "commit");

        Map<String, Object> request = Map.of(
                "buyOrder", buyOrder,
                "sessionId", sessionId,
                "amount", amount,
                "returnUrl", returnUrl);
        model.addAttribute("request", request);
        try {
            var resp = tx.create(buyOrder, sessionId, amount, returnUrl);
            model.addAttribute("response_data", resp);
            model.addAttribute("response_data_json", toJson(resp));
        } catch (Exception e) {
            log.error("ERROR", e);
        }
        return "webpay_plus/create";
    }

}
