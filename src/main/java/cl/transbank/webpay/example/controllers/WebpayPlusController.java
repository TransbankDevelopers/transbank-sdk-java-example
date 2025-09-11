package cl.transbank.webpay.example.controllers;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.exception.TransactionCommitException;
import cl.transbank.webpay.exception.TransactionCreateException;
import cl.transbank.webpay.exception.TransactionRefundException;
import cl.transbank.webpay.exception.TransactionStatusException;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/webpay-plus")
public class WebpayPlusController extends BaseController {
    private static final String TEMPLATE_FOLDER = "webpay_plus";
    private static final String BASE_URL = "/webpay-plus";

    private static final String VIEW_CREATE = TEMPLATE_FOLDER + "/create";
    private static final String VIEW_COMMIT = TEMPLATE_FOLDER + "/commit";
    private static final String VIEW_STATUS = TEMPLATE_FOLDER + "/status";
    private static final String VIEW_REFUND = TEMPLATE_FOLDER + "/refund";

    private static final Map<String, String> NAV_CREATE;
    private static final Map<String, String> NAV_COMMIT;
    private static final Map<String, String> NAV_STATUS;
    private static final Map<String, String> NAV_REFUND;

    static {
        NAV_CREATE = new LinkedHashMap<>();
        NAV_CREATE.put("request", "Petición");
        NAV_CREATE.put("response", "Respuesta");
        NAV_CREATE.put("form", "Formulario");

        NAV_COMMIT = new LinkedHashMap<>();
        NAV_COMMIT.put("data", "Datos recibidos");
        NAV_COMMIT.put("request", "Petición");
        NAV_COMMIT.put("response", "Respuesta");
        NAV_COMMIT.put("operations", "¡Listo!");

        NAV_STATUS = new LinkedHashMap<>();
        NAV_STATUS.put("request", "Petición");
        NAV_STATUS.put("response", "Respuesta");

        NAV_REFUND = NAV_STATUS;
    }

    private final WebpayPlus.Transaction tx;

    public WebpayPlusController() {
        this.tx = new WebpayPlus.Transaction(
                new WebpayOptions(
                        IntegrationCommerceCodes.WEBPAY_PLUS,
                        IntegrationApiKeys.WEBPAY,
                        IntegrationType.TEST
                )
        );
    }

    private void addBreadcrumbs(Model model, String label, String url) {
        Map<String, String> breadcrumbs = new LinkedHashMap<>() {
            {
                put("Inicio", "/");
                put("Webpay Plus", BASE_URL + "/create");
            }
        };
        if (label != null)
            breadcrumbs.put(label, url);
        model.addAttribute("breadcrumbs", breadcrumbs);
    }

    @GetMapping("/create")
    public String create(HttpServletRequest req, Model model) throws TransactionCreateException, IOException {
        model.addAttribute("navigation", NAV_CREATE);
        addBreadcrumbs(model, null, null);

        String buyOrder = "buyOrder_" + getRandomNumber();
        String sessionId = "sessionId_" + getRandomNumber();
        int amount = 1000;
        String returnUrl = req.getRequestURL().toString().replace("create", "commit");

        Map<String, Object> request = Map.of(
                "buyOrder", buyOrder,
                "sessionId", sessionId,
                "amount", amount,
                "returnUrl", returnUrl
        );
        model.addAttribute("request", request);

        var resp = tx.create(buyOrder, sessionId, amount, returnUrl);
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_CREATE;
    }

    @RequestMapping(value = "/commit", method = {RequestMethod.GET, RequestMethod.POST})
    public String commit(
            HttpServletRequest req,
            @RequestParam(name = "token_ws", required = false) String tokenWs,
            @RequestParam(name = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(name = "TBK_ORDEN_COMPRA", required = false) String tbkBuyOrder,
            @RequestParam(name = "TBK_ID_SESION", required = false) String tbkSessionId,
            Model model) throws TransactionCommitException, IOException {

        model.addAttribute("navigation", NAV_COMMIT);
        addBreadcrumbs(model, "Confirmar transacción", "#");

        var resp = tx.commit(tokenWs);
        model.addAttribute("token", tokenWs);
        model.addAttribute("returnUrl", req.getRequestURL().toString());
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_COMMIT;
    }

    @GetMapping("/status")
    public String status(@RequestParam("token_ws") String token, Model model)
            throws IOException, TransactionStatusException {
        model.addAttribute("navigation", NAV_STATUS);
        addBreadcrumbs(model, "Consultar estado de transacción", "#");

        final var resp = tx.status(token);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_STATUS;
    }

    @GetMapping("/refund")
    public String refund(@RequestParam("token_ws") String token,
                         @RequestParam("amount") double amount,
                         Model model) throws TransactionRefundException, IOException {

        model.addAttribute("navigation", NAV_REFUND);
        addBreadcrumbs(model, "Reembolsar", "#");
        model.addAttribute("token", token);

        final var resp = tx.refund(token, amount);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_REFUND;
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("Error inesperado", e);
        model.addAttribute("errorMessage", "Ocurrió un error inesperado.");
        return "error";
    }

}
