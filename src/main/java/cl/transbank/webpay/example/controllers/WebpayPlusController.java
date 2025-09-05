package cl.transbank.webpay.example.controllers;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
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

    @RequestMapping(value = "/commit", method = { RequestMethod.GET, RequestMethod.POST })
    public String commit(
            HttpServletRequest req,
            @RequestParam(name = "token_ws", required = false) String tokenWs,
            @RequestParam(name = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(name = "TBK_ORDEN_COMPRA", required = false) String tbkBuyOrder,
            @RequestParam(name = "TBK_ID_SESION", required = false) String tbkSessionId,
            Model model) {

        Map<String, String> navigation = new LinkedHashMap<>() {
            {
                put("data", "Paso 1 - Datos recibidos");
                put("request", "Paso 2 - Petición");
                put("response", "Paso 3 - Respuesta");
                put("operations", "¡Listo!");
            }
        };
        model.addAttribute("navigation", navigation);

        Map<String, String> breadcrumbs = new LinkedHashMap<>() {
            {
                put("Inicio", "/");
                put("Webpay Plus", "/webpay_plus/create");
                put("Confirmar transacción", "#");
            }
        };
        model.addAttribute("breadcrumbs", breadcrumbs);

        Map<String, Object> details = new HashMap<>();
        model.addAttribute("details", details);

        // TODO: Aborted by user flow

        // TODO: Timeout flow

        // Normal flow
        log.info(String.format("token_ws : %s", tokenWs));
        details.put("token_ws", tokenWs);
        details.put("commit_url", req.getRequestURL().toString());

        try {
            final WebpayPlusTransactionCommitResponse response = tx.commit(tokenWs);
            log.debug(String.format("response : %s", response));

            details.put("response", response);
            details.put("amount", (int) response.getAmount());
            details.put("resp", toJson(response));
            details.put("refund-endpoint", "/webpay_plus/refund");
            details.put("status-endpoint", "/webpay_plus/status");

        } catch (Exception e) {
            log.error("ERROR", e);
            details.put("resp", e.getMessage());
        }

        return "webpay_plus/commit";
    }

    @GetMapping("/status")
    public String status(@RequestParam("token_ws") String token, Model model) {
        Map<String, String> navigation = new LinkedHashMap<>() {
            {
                put("request", "Paso 1 - Petición");
                put("response", "Paso 2 - Respuesta");
            }
        };
        model.addAttribute("navigation", navigation);

        Map<String, String> breadcrumbs = new LinkedHashMap<>() {
            {
                put("Inicio", "/");
                put("Webpay Plus", "/webpay_plus/create");
                put("Consultar estado de transacción", "#");
            }
        };
        model.addAttribute("breadcrumbs", breadcrumbs);

        Map<String, Object> details = new HashMap<>();
        model.addAttribute("details", details);
        details.put("token_ws", token);

        try {
            final var response = tx.status(token);
            details.put("resp", toJson(response));
        } catch (Exception e) {
            log.error("ERROR", e);
            details.put("resp", e.getMessage());
        }

        return "webpay_plus/status";
    }

    @PostMapping("/refund")
    public String refund(@RequestParam("token_ws") String token,
            @RequestParam("amount") double amount,
            Model model) {
        log.info(String.format("token_ws : %s | amount : %s", token, amount));

        Map<String, String> navigation = new LinkedHashMap<>() {
            {
                put("request", "Paso 1 - Petición");
                put("response", "Paso 2 - Respuesta");
            }
        };
        model.addAttribute("navigation", navigation);

        Map<String, String> breadcrumbs = new LinkedHashMap<>() {
            {
                put("Inicio", "/");
                put("Webpay Plus", "/webpay-plus/create");
                put("Reembolsar", "#");
            }
        };
        model.addAttribute("breadcrumbs", breadcrumbs);

        Map<String, Object> details = new HashMap<>();
        model.addAttribute("details", details);
        details.put("token_ws", token);

        try {
            final var response = tx.refund(token, amount);
            details.put("resp", toJson(response));
        } catch (Exception e) {
            log.error("ERROR", e);
            details.put("resp", e.getMessage());
        }

        return "webpay_plus/refund";
    }

}
