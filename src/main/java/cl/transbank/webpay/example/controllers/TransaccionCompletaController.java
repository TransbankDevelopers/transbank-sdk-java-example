package cl.transbank.webpay.example.controllers;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.exception.*;
import cl.transbank.webpay.transaccioncompleta.FullTransaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Log4j2
@Controller
@RequestMapping("/transaccion-completa")
public class TransaccionCompletaController extends BaseController {
    private static final String TEMPLATE_FOLDER = "transaccion_completa";
    private static final String BASE_URL = "/transaccion-completa";
    private static final String PRODUCT = "Webpay Transacción Completa";

    private static final String VIEW_INDEX = TEMPLATE_FOLDER + "/index";
    private static final String VIEW_CREATE = TEMPLATE_FOLDER + "/create";
    private static final String VIEW_INSTALLMENTS = TEMPLATE_FOLDER + "/installments";
    private static final String VIEW_COMMIT = TEMPLATE_FOLDER + "/commit";
    private static final String VIEW_STATUS = TEMPLATE_FOLDER + "/status";
    private static final String VIEW_REFUND = TEMPLATE_FOLDER + "/refund";

    private static final Map<String, String> NAV_INDEX;
    private static final Map<String, String> NAV_CREATE;
    private static final Map<String, String> NAV_INSTALLMENTS;
    private static final Map<String, String> NAV_COMMIT;
    private static final Map<String, String> NAV_STATUS;
    private static final Map<String, String> NAV_REFUND;

    static {
        NAV_INDEX = new LinkedHashMap<>();
        NAV_INDEX.put("form", "Formulario");

        NAV_CREATE = new LinkedHashMap<>();
        NAV_CREATE.put("request", "Petición");
        NAV_CREATE.put("response", "Respuesta");
        NAV_CREATE.put("form", "Formulario");

        NAV_INSTALLMENTS = new LinkedHashMap<>();
        NAV_INSTALLMENTS.put("request", "Petición");
        NAV_INSTALLMENTS.put("response", "Respuesta");
        NAV_INSTALLMENTS.put("form", "Formulario");

        NAV_COMMIT = new LinkedHashMap<>();
        NAV_COMMIT.put("request", "Petición");
        NAV_COMMIT.put("response", "Respuesta");
        NAV_COMMIT.put("form", "Formulario");

        NAV_STATUS = new LinkedHashMap<>();
        NAV_STATUS.put("request", "Petición");
        NAV_STATUS.put("response", "Respuesta");

        NAV_REFUND = NAV_STATUS;
    }

    private final FullTransaction tx;

    public TransaccionCompletaController() {
        this.tx = new FullTransaction(
                new WebpayOptions(
                        IntegrationCommerceCodes.TRANSACCION_COMPLETA,
                        IntegrationApiKeys.WEBPAY,
                        IntegrationType.TEST
                )
        );
    }

    private void addProductAndBreadcrumbs(Model model, String label, String url) {
        var breadcrumbs = new LinkedHashMap<String, String>();
        breadcrumbs.put("Inicio", "/");
        breadcrumbs.put(PRODUCT, BASE_URL);
        if (label != null) {
            breadcrumbs.put(label, url);
        }
        model.addAttribute("product", PRODUCT);
        model.addAttribute("breadcrumbs", breadcrumbs);
    }

    @GetMapping("")
    public String index(Model model) {
        model.addAttribute("navigation", NAV_INDEX);
        addProductAndBreadcrumbs(model, null, null);
        return VIEW_INDEX;
    }

    @PostMapping("/create")
    public String create(
            HttpServletRequest req,
            @RequestParam("number") String number,
            @RequestParam("expiry") String expiry,
            @RequestParam("cvc") String cvc,
            Model model
    ) throws TransactionCreateException, IOException {
        model.addAttribute("navigation", NAV_CREATE);
        addProductAndBreadcrumbs(model, "Crear transacción", BASE_URL + "/create");

        String cardNumber = number.replaceAll("\\s+", "");
        String[] expiryParts = expiry.split("/");
        String month = expiryParts.length > 0 ? expiryParts[0] : "";
        String year = expiryParts.length > 1 ? expiryParts[1] : "";
        String cardExpiry = year + "/" + month;

        String buyOrder = "O-" + getRandomNumber();
        String sessionId = "S-" + getRandomNumber();
        double amount = 1000 + ThreadLocalRandom.current().nextInt(1001);

        var resp = tx.create(buyOrder, sessionId, amount, Short.parseShort(cvc), cardNumber, cardExpiry);
        req.getSession().setAttribute("transaccion_completa_amount", amount);

        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_CREATE;
    }

    @PostMapping("/installments")
    public String installments(
            @RequestParam("token") String token,
            @RequestParam("installments_number") byte installmentsNumber,
            Model model
    ) throws TransactionInstallmentException, IOException {
        model.addAttribute("navigation", NAV_INSTALLMENTS);
        addProductAndBreadcrumbs(model, "Consulta de cuotas", BASE_URL + "/installments");

        var resp = tx.installments(token, installmentsNumber);
        model.addAttribute("request_token", token);
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_INSTALLMENTS;
    }

    @GetMapping("/commit")
    public String commit(
            HttpServletRequest req,
            @RequestParam("token") String token,
            @RequestParam(value = "idQueryInstallments", required = false) Long idQueryInstallments,
            Model model
    ) throws TransactionCommitException, IOException {
        model.addAttribute("navigation", NAV_COMMIT);
        addProductAndBreadcrumbs(model, "Confirmar transacción", BASE_URL + "/commit");

        Byte deferredPeriodIndex = null;
        Boolean gracePeriod = Boolean.FALSE;

        var resp = tx.commit(token, idQueryInstallments, deferredPeriodIndex, gracePeriod);
        Object amount = req.getSession().getAttribute("transaccion_completa_amount");
        req.getSession().removeAttribute("transaccion_completa_amount");

        model.addAttribute("amount", amount);
        model.addAttribute("request_token", token);
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_COMMIT;
    }

    @GetMapping("/status")
    public String status(
            @RequestParam("token") String token,
            Model model
    ) throws TransactionStatusException, IOException {
        model.addAttribute("navigation", NAV_STATUS);
        addProductAndBreadcrumbs(model, "Estado de transacción", BASE_URL + "/status");

        var resp = tx.status(token);
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_STATUS;
    }

    @GetMapping("/refund")
    public String refund(
            @RequestParam("token") String token,
            @RequestParam("amount") double amount,
            Model model
    ) throws TransactionRefundException, IOException {
        model.addAttribute("navigation", NAV_REFUND);
        addProductAndBreadcrumbs(model, "Reembolsar", BASE_URL + "/refund");

        var resp = tx.refund(token, amount);
        model.addAttribute("request_token", token);
        model.addAttribute("response_data", resp);
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
