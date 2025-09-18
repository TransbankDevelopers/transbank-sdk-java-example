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
    private static final String PRODUCT = "Webpay Plus";

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

    private void addProductAndBreadcrumbs(Model model, String label, String url) {
        var breadcrumbs = new LinkedHashMap<String, String>();
        breadcrumbs.put("Inicio", "/");
        breadcrumbs.put("Webpay Plus", BASE_URL + "/create");
        if (label != null)
            breadcrumbs.put(label, url);
        model.addAttribute("product", PRODUCT);
        model.addAttribute("breadcrumbs", breadcrumbs);
    }

    @GetMapping("/create")
    public String create(HttpServletRequest req, Model model) throws TransactionCreateException, IOException {
        model.addAttribute("navigation", NAV_CREATE);
        addProductAndBreadcrumbs(model, null, null);

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

    @GetMapping(value = "/commit")
    public String commit(
            HttpServletRequest req,
            @RequestParam Map<String, String> params,
            @RequestParam(name = "token_ws", required = false) String tokenWs,
            @RequestParam(name = "TBK_TOKEN", required = false) String tbkToken,
            Model model) throws TransactionCommitException, IOException, TransactionStatusException {
        return commitBase(req, params, tokenWs, tbkToken, model);
    }

    @PostMapping(value = "/commit")
    public String commitPost(
            HttpServletRequest req,
            @RequestParam Map<String, String> params,
            @RequestParam(name = "token_ws", required = false) String tokenWs,
            @RequestParam(name = "TBK_TOKEN", required = false) String tbkToken,
            Model model) throws TransactionCommitException, IOException, TransactionStatusException {
        return commitBase(req, params, tokenWs, tbkToken, model);
    }

    public String commitBase(
            HttpServletRequest req,
            Map<String, String> params,
            String tokenWs,
            String tbkToken,
            Model model) throws TransactionCommitException, IOException, TransactionStatusException {

        String viewTemplate = VIEW_COMMIT;
        model.addAttribute("request_data_json", toJson(params));
        model.addAttribute("navigation", NAV_COMMIT);
        addProductAndBreadcrumbs(model, "Confirmar transacción", "#");

        if (tbkToken != null && tokenWs != null) {
            viewTemplate = VIEW_FORM_ERROR;
        } else if (tbkToken != null) {
            viewTemplate = VIEW_ABORTED_ERROR;
            var resp = tx.status(tbkToken);
            model.addAttribute("response_data_json", toJson(resp));
        } else if (tokenWs != null) {
            var resp = tx.commit(tokenWs);
            model.addAttribute("token", tokenWs);
            model.addAttribute("returnUrl", req.getRequestURL().toString());
            model.addAttribute("response_data", resp);
            model.addAttribute("response_data_json", toJson(resp));
        } else {
            viewTemplate = VIEW_TIMEOUT_ERROR;
        }
        return viewTemplate;
    }

    @GetMapping("/status")
    public String status(@RequestParam("token_ws") String token, Model model)
            throws IOException, TransactionStatusException {
        model.addAttribute("navigation", NAV_STATUS);
        addProductAndBreadcrumbs(model, "Consultar estado de transacción", "#");

        final var resp = tx.status(token);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_STATUS;
    }

    @GetMapping("/refund")
    public String refund(@RequestParam("token_ws") String token,
                         @RequestParam("amount") double amount,
                         Model model) throws TransactionRefundException, IOException {

        model.addAttribute("navigation", NAV_REFUND);
        addProductAndBreadcrumbs(model, "Reembolsar", "#");
        model.addAttribute("token", token);

        final var resp = tx.refund(token, amount);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_REFUND;
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("Error inesperado", e);
        model.addAttribute("error", e.getMessage());
        return VIEW_ERROR;
    }

}
