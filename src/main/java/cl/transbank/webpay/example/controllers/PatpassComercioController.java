package cl.transbank.webpay.example.controllers;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.patpass.PatpassComercio;
import cl.transbank.patpass.responses.PatpassComercioInscriptionStartResponse;
import cl.transbank.patpass.responses.PatpassComercioTransactionStatusResponse;
import cl.transbank.webpay.exception.InscriptionStartException;
import cl.transbank.webpay.exception.TransactionStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/patpass-comercio")
public class PatpassComercioController extends BaseController {
    private static final String TEMPLATE_FOLDER = "patpass_comercio";
    private static final String BASE_URL = "/patpass-comercio";
    private static final String PRODUCT = "Patpass Comercio";
    private static final String SESSION_TOKEN_KEY = "patpass_j_token";
    private static final String DEFAULT_VOUCHER_URL =
            "https://pagoautomaticocontarjetasint.transbank.cl/nuevo-ic-rest/tokenVoucherLogin";

    private static final String COMMIT_PATH = "/commit";
    private static final String VOUCHER_PATH = "/voucher";
    private static final String NAVIGATION_ATTR = "navigation";
    private static final String VOUCHER_URL = "voucherUrl";
    private static final String ERROR_ATTR = "error";
    private static final String VIEW_START = TEMPLATE_FOLDER + "/start";
    private static final String VIEW_COMMIT = TEMPLATE_FOLDER + COMMIT_PATH;
    private static final String VIEW_VOUCHER = TEMPLATE_FOLDER + VOUCHER_PATH;

    private static final Map<String, String> NAV_START;
    private static final Map<String, String> NAV_COMMIT;
    private static final Map<String, String> NAV_VOUCHER;

    static {
        NAV_START = new LinkedHashMap<>();
        NAV_START.put("request", "Petición");
        NAV_START.put("response", "Respuesta");
        NAV_START.put("form", "Formulario");
        NAV_START.put("example", "Ejemplo");

        NAV_COMMIT = new LinkedHashMap<>();
        NAV_COMMIT.put("data", "Datos recibidos");
        NAV_COMMIT.put("request", "Petición");
        NAV_COMMIT.put("response", "Respuesta");
        NAV_COMMIT.put("form", "Formulario");

        NAV_VOUCHER = new LinkedHashMap<>();
        NAV_VOUCHER.put("form", "voucher");
    }

    private final PatpassComercio.Inscription inscription;

    public PatpassComercioController() {
        this.inscription = PatpassComercio.Inscription.buildForIntegration(
                IntegrationCommerceCodes.PATPASS_COMERCIO,
                IntegrationApiKeys.PATPASS_COMERCIO
        );
    }

    private void addBreadcrumbs(Model model, String label, String url) {
        Map<String, String> breadcrumbs = new LinkedHashMap<>();
        breadcrumbs.put("Inicio", "/");
        breadcrumbs.put(PRODUCT, BASE_URL);
        if (label != null) {
            breadcrumbs.put(label, url);
        }
        model.addAttribute("product", PRODUCT);
        model.addAttribute("breadcrumbs", breadcrumbs);
    }

    @GetMapping({"", "/"})
    public String start(HttpServletRequest request, Model model)
            throws IOException, InscriptionStartException {
        model.addAttribute(NAVIGATION_ATTR, NAV_START);
        addBreadcrumbs(model, null, null);

        String returnUrl = request.getRequestURL().toString().replaceAll("/?$", "") + COMMIT_PATH;
        String finalUrl = request.getRequestURL().toString().replaceAll("/?$", "") + VOUCHER_PATH;

        Map<String, Object> requestData = new LinkedHashMap<>();
        requestData.put("serviceId", "Service-" + getRandomNumber());
        requestData.put("maxAmount", 100);
        requestData.put("returnUrl", returnUrl);
        requestData.put("finalUrl", finalUrl);
        requestData.put("name", "Isaac");
        requestData.put("lastName", "Newton");
        requestData.put("secondLastName", "Gonzales");
        requestData.put("rut", "11111111-1");
        requestData.put("phone", "123456734");
        requestData.put("cellPhone", "123456723");
        requestData.put("patpassName", "Membresia de cable");
        requestData.put("personEmail", "developer@continuum.cl");
        requestData.put("commerceEmail", "developer@continuum.cl");
        requestData.put("address", "Satelite 101");
        requestData.put("city", "Santiago");


        PatpassComercioInscriptionStartResponse resp = this.inscription.start(
                requestData.get("returnUrl").toString(),
                requestData.get("name").toString(),
                requestData.get("lastName").toString(),
                requestData.get("secondLastName").toString(),
                requestData.get("rut").toString(),
                requestData.get("serviceId").toString(),
                requestData.get("finalUrl").toString(),
                Double.valueOf(requestData.get("maxAmount").toString()),
                requestData.get("phone").toString(),
                requestData.get("cellPhone").toString(),
                requestData.get("patpassName").toString(),
                requestData.get("personEmail").toString(),
                requestData.get("commerceEmail").toString(),
                requestData.get("address").toString(),
                requestData.get("city").toString()
        );

        model.addAttribute("request_data", requestData);
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));
        return VIEW_START;
    }

    @PostMapping("/commit")
    public String commitPost(@RequestParam(name = "j_token", required = false) String jTokenLower,
                             @RequestParam(name = "J_TOKEN", required = false) String jTokenUpper,
                             @RequestParam(name = "token", required = false) String token,
                             HttpServletRequest request,
                             Model model) {
        String jToken = firstNonBlank(jTokenLower, jTokenUpper, token);
        if (isBlank(jToken)) {
            model.addAttribute(ERROR_ATTR, "No se recibió el token de inscripción (J_TOKEN).");
            return VIEW_ERROR;
        }

        request.getSession().setAttribute(SESSION_TOKEN_KEY, jToken);
        return "redirect:" + BASE_URL + COMMIT_PATH;
    }

    @GetMapping(COMMIT_PATH)
    public String commit(@RequestParam(name = "j_token", required = false) String jTokenLower,
                         @RequestParam(name = "J_TOKEN", required = false) String jTokenUpper,
                         @RequestParam(name = "token", required = false) String token,
                         HttpServletRequest request,
                         Model model)
            throws IOException, TransactionStatusException {
        String jToken = getIncomingToken(request.getSession(), jTokenLower, jTokenUpper, token);
        if (isBlank(jToken)) {
            model.addAttribute(ERROR_ATTR, "No se encontró el token de inscripción (J_TOKEN).");
            return VIEW_ERROR;
        }

        model.addAttribute(NAVIGATION_ATTR, NAV_COMMIT);
        addBreadcrumbs(model, "Confirmar registro", BASE_URL + COMMIT_PATH);

        PatpassComercioTransactionStatusResponse resp = inscription.status(jToken);
        request.getSession().setAttribute(SESSION_TOKEN_KEY, jToken);

        model.addAttribute("token", jToken);
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));
        model.addAttribute("response_payload_json", toJson(Map.of(
                "authorized", resp.isAuthorized(),
                VOUCHER_URL, resp.getVoucherUrl()
        )));
        model.addAttribute(VOUCHER_URL,
                isBlank(resp.getVoucherUrl()) ? DEFAULT_VOUCHER_URL : resp.getVoucherUrl());

        return VIEW_COMMIT;
    }

    @PostMapping(VOUCHER_PATH)
    public String voucherPost(@RequestParam(name = "j_token", required = false) String jTokenLower,
                              @RequestParam(name = "J_TOKEN", required = false) String jTokenUpper,
                              @RequestParam(name = "tokenComercio", required = false) String tokenComercio,
                              @RequestParam(name = "token", required = false) String token,
                              HttpServletRequest request,
                              Model model) {
        String jToken = firstNonBlank(jTokenLower, jTokenUpper, tokenComercio, token);
        if (isBlank(jToken)) {
            model.addAttribute(ERROR_ATTR, "No se recibió el token de inscripción (J_TOKEN).");
            return VIEW_ERROR;
        }

        request.getSession().setAttribute(SESSION_TOKEN_KEY, jToken);
        return "redirect:" + BASE_URL + VOUCHER_PATH;
    }

    @GetMapping(VOUCHER_PATH)
    public String voucher(@RequestParam(name = "j_token", required = false) String jTokenLower,
                          @RequestParam(name = "J_TOKEN", required = false) String jTokenUpper,
                          @RequestParam(name = "tokenComercio", required = false) String tokenComercio,
                          @RequestParam(name = "token", required = false) String token,
                          HttpServletRequest request,
                          Model model) {
        String jToken = getIncomingToken(request.getSession(), jTokenLower, jTokenUpper, tokenComercio, token);
        if (isBlank(jToken)) {
            model.addAttribute(ERROR_ATTR, "No se encontró el token de inscripción (J_TOKEN).");
            return VIEW_ERROR;
        }

        model.addAttribute(NAVIGATION_ATTR, NAV_VOUCHER);
        addBreadcrumbs(model, "Voucher", BASE_URL + VOUCHER_PATH);
        model.addAttribute("token", jToken);
        model.addAttribute(VOUCHER_URL, DEFAULT_VOUCHER_URL);

        return VIEW_VOUCHER;
    }

    private String getIncomingToken(HttpSession session, String... values) {
        String token = firstNonBlank(values);
        if (!isBlank(token)) {
            return token;
        }
        Object sessionToken = session.getAttribute(SESSION_TOKEN_KEY);
        return sessionToken == null ? null : sessionToken.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("Error inesperado", e);
        model.addAttribute(ERROR_ATTR, getDisplayableErrorMessage(e));
        return VIEW_ERROR;
    }
}
