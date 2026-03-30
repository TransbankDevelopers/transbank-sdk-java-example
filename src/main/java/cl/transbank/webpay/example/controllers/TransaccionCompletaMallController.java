package cl.transbank.webpay.example.controllers;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.model.MallTransactionCreateDetails;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.exception.*;
import cl.transbank.webpay.transaccioncompleta.MallFullTransaction;
import cl.transbank.webpay.transaccioncompleta.model.MallTransactionCommitDetails;
import cl.transbank.webpay.transaccioncompleta.responses.MallFullTransactionInstallmentsDetails;
import cl.transbank.webpay.example.models.MallDetailSession;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/transaccion-completa-mall")
public class TransaccionCompletaMallController extends BaseController {
    private static final String TEMPLATE_FOLDER = "transaccion_completa_mall";
    private static final String BASE_URL = "/transaccion-completa-mall";
    private static final String PRODUCT = "Webpay Transacción Completa Mall";

    private static final String VIEW_INDEX = TEMPLATE_FOLDER + "/index";
    private static final String VIEW_CREATE = TEMPLATE_FOLDER + "/create";
    private static final String VIEW_INSTALLMENTS = TEMPLATE_FOLDER + "/installments";
    private static final String VIEW_COMMIT = TEMPLATE_FOLDER + "/commit";
    private static final String VIEW_STATUS = TEMPLATE_FOLDER + "/status";
    private static final String VIEW_REFUND = TEMPLATE_FOLDER + "/refund";

    private static final String NAV_LABEL_FORM = "Formulario";
    private static final String NAV_LABEL_REQUEST = "Petición";
    private static final String NAV_LABEL_RESPONSE = "Respuesta";

    private static final String ATTR_NAVIGATION = "navigation";
    private static final String ATTR_PRODUCT = "product";
    private static final String ATTR_BREADCRUMBS = "breadcrumbs";
    private static final String ATTR_RESPONSE_DATA = "response_data";
    private static final String ATTR_RESPONSE_DATA_JSON = "response_data_json";
    private static final String ATTR_REQUEST_TOKEN = "request_token";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_ID_QUERY_INSTALLMENTS = "id_query_installments";

    private static final String SESSION_DETAILS = "transaccion_completa_mall_details";
    private static final String NAV_KEY_REQUEST = "request";
    private static final String NAV_KEY_RESPONSE = "response";
    private static final String NAV_KEY_FORM = "form";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Map<String, String> NAV_INDEX = createNav(NAV_KEY_FORM);
    private static final Map<String, String> NAV_CREATE = createNav(NAV_KEY_REQUEST, NAV_KEY_RESPONSE, NAV_KEY_FORM);
    private static final Map<String, String> NAV_INSTALLMENTS = createNav(NAV_KEY_REQUEST, NAV_KEY_RESPONSE, NAV_KEY_FORM);
    private static final Map<String, String> NAV_COMMIT = createNav(NAV_KEY_REQUEST, NAV_KEY_RESPONSE, NAV_KEY_FORM);
    private static final Map<String, String> NAV_STATUS = createNav(NAV_KEY_REQUEST, NAV_KEY_RESPONSE);
    private static final Map<String, String> NAV_REFUND = NAV_STATUS;

    private static Map<String, String> createNav(String... keys) {
        Map<String, String> nav = new LinkedHashMap<>();
        for (String key : keys) {
            switch (key) {
                case NAV_KEY_REQUEST -> nav.put(key, NAV_LABEL_REQUEST);
                case NAV_KEY_RESPONSE -> nav.put(key, NAV_LABEL_RESPONSE);
                case NAV_KEY_FORM -> nav.put(key, NAV_LABEL_FORM);
                default -> { }
            }
        }
        return nav;
    }

    private final MallFullTransaction tx;

    public TransaccionCompletaMallController() {
        this.tx = new MallFullTransaction(
                new WebpayOptions(
                        IntegrationCommerceCodes.TRANSACCION_COMPLETA_MALL,
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
        model.addAttribute(ATTR_PRODUCT, PRODUCT);
        model.addAttribute(ATTR_BREADCRUMBS, breadcrumbs);
    }

    @GetMapping("")
    public String index(Model model) {
        model.addAttribute(ATTR_NAVIGATION, NAV_INDEX);
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
        model.addAttribute(ATTR_NAVIGATION, NAV_CREATE);
        addProductAndBreadcrumbs(model, "Crear transacción", BASE_URL + "/create");

        String cardNumber = number.replaceAll("\\s+", "");
        String[] expiryParts = expiry.split("/");
        String month = expiryParts.length > 0 ? expiryParts[0] : "";
        String year = expiryParts.length > 1 ? expiryParts[1] : "";
        String cardExpiry = year + "/" + month;

        String buyOrder = "O-" + getRandomNumber();
        String sessionId = "S-" + getRandomNumber();

        var sessionDetails = buildSessionDetails(
                IntegrationCommerceCodes.TRANSACCION_COMPLETA_MALL_CHILD1,
                IntegrationCommerceCodes.TRANSACCION_COMPLETA_MALL_CHILD2
        );

        var details = MallTransactionCreateDetails.build()
                .add(sessionDetails.get(0).getAmount(), sessionDetails.get(0).getCommerceCode(), sessionDetails.get(0).getBuyOrder())
                .add(sessionDetails.get(1).getAmount(), sessionDetails.get(1).getCommerceCode(), sessionDetails.get(1).getBuyOrder());

        var resp = tx.create(buyOrder, sessionId, cardNumber, cardExpiry, details, Short.parseShort(cvc));
        req.getSession().setAttribute(SESSION_DETAILS, sessionDetails);

        model.addAttribute(ATTR_RESPONSE_DATA, resp);
        model.addAttribute(ATTR_RESPONSE_DATA_JSON, toJson(resp));

        return VIEW_CREATE;
    }

    @PostMapping("/installments")
    public String installments(
            HttpServletRequest req,
            @RequestParam("token") String token,
            @RequestParam("installments_number") byte installmentsNumber,
            Model model
    ) throws TransactionInstallmentException, IOException {
        model.addAttribute(ATTR_NAVIGATION, NAV_INSTALLMENTS);
        addProductAndBreadcrumbs(model, "Consulta de cuotas", BASE_URL + "/installments");

        List<MallDetailSession> sessionDetails = getSessionDetails(req);
        if (sessionDetails.isEmpty()) {
            model.addAttribute(ATTR_ERROR, "Debes crear la transacción antes de consultar cuotas.");
            return VIEW_ERROR;
        }

        var details = MallFullTransactionInstallmentsDetails.build()
                .add(sessionDetails.get(0).getCommerceCode(), sessionDetails.get(0).getBuyOrder(), installmentsNumber)
                .add(sessionDetails.get(1).getCommerceCode(), sessionDetails.get(1).getBuyOrder(), installmentsNumber);

        var resp = tx.installments(token, details);
        Long idQueryInstallments = null;
        if (resp != null && resp.getResponseList() != null && !resp.getResponseList().isEmpty()) {
            idQueryInstallments = resp.getResponseList().get(0).getIdQueryInstallments();
        }

        model.addAttribute(ATTR_REQUEST_TOKEN, token);
        model.addAttribute(ATTR_RESPONSE_DATA, resp);
        model.addAttribute(ATTR_RESPONSE_DATA_JSON, toJson(resp));
        model.addAttribute(ATTR_ID_QUERY_INSTALLMENTS, idQueryInstallments);

        return VIEW_INSTALLMENTS;
    }

    @GetMapping("/commit")
    public String commit(
            HttpServletRequest req,
            @RequestParam("token") String token,
            @RequestParam(value = "idQueryInstallments", required = false) Long idQueryInstallments,
            @RequestParam(value = "deferredPeriodIndex", required = false) Byte deferredPeriodIndex,
            @RequestParam(value = "gracePeriod", required = false) Boolean gracePeriod,
            Model model
    ) throws TransactionCommitException, IOException {
        model.addAttribute(ATTR_NAVIGATION, NAV_COMMIT);
        addProductAndBreadcrumbs(model, "Confirmar transacción", BASE_URL + "/commit");

        List<MallDetailSession> sessionDetails = getSessionDetails(req);
        if (sessionDetails.isEmpty()) {
            model.addAttribute(ATTR_ERROR, "Debes crear la transacción antes de confirmar.");
            return VIEW_ERROR;
        }

        boolean safeGracePeriod = gracePeriod != null ? gracePeriod : Boolean.FALSE;

        var details = MallTransactionCommitDetails.build()
                .add(sessionDetails.get(0).getCommerceCode(), sessionDetails.get(0).getBuyOrder(), idQueryInstallments, deferredPeriodIndex, safeGracePeriod)
                .add(sessionDetails.get(1).getCommerceCode(), sessionDetails.get(1).getBuyOrder(), idQueryInstallments, deferredPeriodIndex, safeGracePeriod);

        var resp = tx.commit(token, details);

        model.addAttribute(ATTR_REQUEST_TOKEN, token);
        model.addAttribute(ATTR_RESPONSE_DATA, resp);
        model.addAttribute(ATTR_RESPONSE_DATA_JSON, toJson(resp));

        return VIEW_COMMIT;
    }

    @GetMapping("/status")
    public String status(
            @RequestParam("token") String token,
            Model model
    ) throws TransactionStatusException, IOException {
        model.addAttribute(ATTR_NAVIGATION, NAV_STATUS);
        addProductAndBreadcrumbs(model, "Estado de transacción", BASE_URL + "/status");

        var resp = tx.status(token);
        model.addAttribute(ATTR_RESPONSE_DATA, resp);
        model.addAttribute(ATTR_RESPONSE_DATA_JSON, toJson(resp));

        return VIEW_STATUS;
    }

    @GetMapping("/refund")
    public String refund(
            @RequestParam("token") String token,
            @RequestParam("buy_order") String buyOrder,
            @RequestParam("commerce_code") String commerceCode,
            @RequestParam("amount") double amount,
            Model model
    ) throws TransactionRefundException, IOException {
        model.addAttribute(ATTR_NAVIGATION, NAV_REFUND);
        addProductAndBreadcrumbs(model, "Reembolsar", BASE_URL + "/refund");

        var resp = tx.refund(token, buyOrder, commerceCode, amount);
        model.addAttribute(ATTR_REQUEST_TOKEN, token);
        model.addAttribute(ATTR_RESPONSE_DATA, resp);
        model.addAttribute(ATTR_RESPONSE_DATA_JSON, toJson(resp));

        return VIEW_REFUND;
    }

    private List<MallDetailSession> buildSessionDetails(String commerceCode1, String commerceCode2) {
        List<MallDetailSession> details = new ArrayList<>();
        details.add(new MallDetailSession(
                1000.0 + SECURE_RANDOM.nextInt(1001),
                commerceCode1,
                "O-" + getRandomNumber()
        ));
        details.add(new MallDetailSession(
                1000.0 + SECURE_RANDOM.nextInt(1001),
                commerceCode2,
                "O-" + getRandomNumber()
        ));
        return details;
    }

    @SuppressWarnings("unchecked")
    private List<MallDetailSession> getSessionDetails(HttpServletRequest req) {
        Object value = req.getSession().getAttribute(SESSION_DETAILS);
        if (value instanceof List<?>) {
            return (List<MallDetailSession>) value;
        }
        return new ArrayList<>();
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("Error inesperado", e);
        model.addAttribute(ATTR_ERROR, e.getMessage());
        return VIEW_ERROR;
    }
}
