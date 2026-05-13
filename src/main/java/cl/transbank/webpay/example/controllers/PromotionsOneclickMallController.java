package cl.transbank.webpay.example.controllers;

import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.exception.*;
import cl.transbank.webpay.oneclick.Oneclick;
import cl.transbank.webpay.oneclick.model.MallTransactionCreateDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/promotions-oneclick-mall")
public class PromotionsOneclickMallController extends BaseController {

    private static final int AUTHORIZED = 0;
    private static final String TEMPLATE_FOLDER = "promotions_oneclick_mall";
    private static final String BASE_URL = "/promotions-oneclick-mall";
    private static final String PRODUCT = "Webpay Oneclick Mall Promociones";
    private static final String MODEL_NAVIGATION = "navigation";
    private static final String MODEL_RESPONSE = "response_data";
    private static final String MODEL_RESPONSE_JSON = "response_data_json";
    private static final String REQUEST_DATA_JSON = "request_data_json";
    private static final String REQUEST = "Petición";
    private static final String RESPONSE = "Respuesta";

    private static final String VIEW_START = TEMPLATE_FOLDER + "/start";
    private static final String VIEW_FINISH = TEMPLATE_FOLDER + "/finish";
    private static final String VIEW_AUTHORIZE = TEMPLATE_FOLDER + "/authorize";
    private static final String VIEW_DELETE = TEMPLATE_FOLDER + "/delete";
    private static final String VIEW_STATUS = TEMPLATE_FOLDER + "/status";
    private static final String VIEW_REFUND = TEMPLATE_FOLDER + "/refund";
    private static final String VIEW_INFO_BIN = TEMPLATE_FOLDER + "/info_bin";

    private static final String ENV_API_KEY = "ONECLICK_MALL_PROMOTIONS_API_KEY";
    private static final String ENV_COMMERCE_CODE = "ONECLICK_MALL_PROMOTIONS_COMMERCE_CODE";
    private static final String ENV_CHILD1_COMMERCE_CODE = "ONECLICK_MALL_PROMOTIONS_CHILD1_COMMERCE_CODE";
    private static final String ENV_CHILD2_COMMERCE_CODE = "ONECLICK_MALL_PROMOTIONS_CHILD2_COMMERCE_CODE";
    private static final String TBK_USER = "tbkUser";
    private static final String REQUEST_KEY = "request";
    private static final String RESPONSE_KEY = "response";
    private static final String DATA_KEY = "Datos";
    private static final String USERNAME = "username";
    private static final String REQUEST_DATA = "request_data";

    private static final Map<String, String> NAV_START = navigation(
            REQUEST_KEY, REQUEST,
            RESPONSE_KEY, RESPONSE,
            "form", "Creación del formulario",
            "example", "Ejemplo"
    );
    private static final Map<String, String> NAV_FINISH = navigation(
            "data", DATA_KEY,
            REQUEST_KEY, REQUEST,
            RESPONSE_KEY, RESPONSE,
            "authorize", "Autorizar una transacción"
    );
    private static final Map<String, String> NAV_FINISH_RECOVER = navigation("data", DATA_KEY);
    private static final Map<String, String> NAV_FINISH_REJECTED = navigation(
            "data", DATA_KEY,
            REQUEST_KEY, REQUEST,
            RESPONSE_KEY, RESPONSE
    );
    private static final Map<String, String> NAV_AUTHORIZE = navigation(
            REQUEST_KEY, REQUEST,
            RESPONSE_KEY, RESPONSE,
            "done", "Listo"
    );
    private static final Map<String, String> NAV_DELETE = navigation(REQUEST_KEY, REQUEST, RESPONSE_KEY, RESPONSE);
    private static final Map<String, String> NAV_TWO_STEP = navigation(REQUEST_KEY, REQUEST, RESPONSE_KEY, RESPONSE);
    private static final Map<String, String> DOTENV = loadDotenv();

    @Value("${oneclick.mall.promotions.api-key:}")
    private String apiKey;

    @Value("${oneclick.mall.promotions.commerce-code:}")
    private String commerceCode;

    @Value("${oneclick.mall.promotions.child1-commerce-code:}")
    private String child1CommerceCode;

    @Value("${oneclick.mall.promotions.child2-commerce-code:}")
    private String child2CommerceCode;

    @GetMapping({"", "/", "/start"})
    public String start(HttpServletRequest req, Model model)
            throws IOException, InscriptionStartException {
        addPageMetadata(model, NAV_START, "Iniciar inscripción");

        String username = "User-" + getRandomNumber();
        String email = "user." + getRandomNumber() + "@example.com";
        String requestUrl = req.getRequestURL().toString();
        String returnUrl = requestUrl.replaceFirst("/start/?$", "").replaceFirst("/$", "") + "/finish";

        var resp = getInscription().start(username, email, returnUrl);

        Map<String, String> requestData = Map.of(
                USERNAME, username,
                "email", email,
                "returnUrl", returnUrl
        );

        model.addAttribute(REQUEST_DATA, requestData);
        model.addAttribute(REQUEST_DATA_JSON, toJson(requestData));
        model.addAttribute(MODEL_RESPONSE, resp);
        model.addAttribute(MODEL_RESPONSE_JSON, toJson(resp));

        req.getSession().setAttribute(USERNAME, username);
        req.getSession().setAttribute("email", email);

        return VIEW_START;
    }

    @GetMapping("/finish")
    public String finish(HttpServletRequest req,
                         @RequestParam Map<String, String> params,
                         @RequestParam(name = "TBK_TOKEN", required = false) String token,
                         @RequestParam(name = "TBK_ORDEN_COMPRA", required = false) String ordenCompra,
                         Model model)
            throws IOException, InscriptionFinishException {
        addPageMetadata(model, NAV_FINISH, "Finalizar inscripción");

        if (ordenCompra != null) {
            addNavigation(model, NAV_FINISH_RECOVER);
            model.addAttribute(REQUEST_DATA_JSON, toJson(params));
            return VIEW_RECOVER_ERROR;
        }

        String username = (String) req.getSession().getAttribute(USERNAME);
        var resp = getInscription().finish(token);

        model.addAttribute(MODEL_RESPONSE, resp);
        model.addAttribute(MODEL_RESPONSE_JSON, toJson(resp));

        if (resp.getResponseCode() != AUTHORIZED) {
            addNavigation(model, NAV_FINISH_REJECTED);
            model.addAttribute(REQUEST_DATA_JSON, toJson(params));
            return VIEW_REJECTED_ERROR;
        }

        req.getSession().setAttribute(TBK_USER, resp.getTbkUser());

        model.addAttribute(REQUEST_DATA, Map.of(
                USERNAME, username,
                TBK_USER, resp.getTbkUser()
        ));
        model.addAttribute("token", token);
        model.addAttribute(USERNAME, username);
        model.addAttribute("tbk_user", resp.getTbkUser());
        model.addAttribute("child_commerce_code1", getConfiguredValue(child1CommerceCode, ENV_CHILD1_COMMERCE_CODE));
        model.addAttribute("child_commerce_code2", getConfiguredValue(child2CommerceCode, ENV_CHILD2_COMMERCE_CODE));

        return VIEW_FINISH;
    }

    @GetMapping("/delete")
    public String delete(@RequestParam String username,
                         @RequestParam("tbk_user") String tbkUser,
                         Model model)
            throws IOException, InscriptionDeleteException {
        addPageMetadata(model, NAV_DELETE, "Eliminar inscripción");
        getInscription().delete(tbkUser, username);
        return VIEW_DELETE;
    }

    @GetMapping("/authorize")
    public String authorize(
            @RequestParam String username,
            @RequestParam("tbk_user") String tbkUser,
            @RequestParam("child_commerce_code1") String childCode1,
            @RequestParam("child_commerce_code2") String childCode2,
            @RequestParam("child_commerce_amount1") double amount1,
            @RequestParam("child_commerce_amount2") double amount2,
            @RequestParam("child_commerce_installments1") int installments1,
            @RequestParam("child_commerce_installments2") int installments2,
            Model model)
            throws IOException, TransactionAuthorizeException {
        addPageMetadata(model, NAV_AUTHORIZE, "Autorizar transacción");

        String buyOrder = "buyOrder_" + getRandomNumber();
        String childBuyOrder1 = "childBuyOrder1_" + getRandomNumber();
        String childBuyOrder2 = "childBuyOrder2_" + getRandomNumber();

        var details = MallTransactionCreateDetails
                .build()
                .add(amount1, childCode1, childBuyOrder1, (byte) installments1)
                .add(amount2, childCode2, childBuyOrder2, (byte) installments2);

        var resp = getTransaction().authorize(username, tbkUser, buyOrder, details);
        model.addAttribute(MODEL_RESPONSE, resp);
        model.addAttribute(MODEL_RESPONSE_JSON, toJson(resp));
        return VIEW_AUTHORIZE;
    }

    @GetMapping("/status")
    public String status(@RequestParam("buy_order") String buyOrder, Model model)
            throws IOException, TransactionStatusException {
        addPageMetadata(model, NAV_TWO_STEP, "Consultar estado");
        var resp = getTransaction().status(buyOrder);
        model.addAttribute(MODEL_RESPONSE_JSON, toJson(resp));
        return VIEW_STATUS;
    }

    @GetMapping("/refund")
    public String refund(@RequestParam("buy_order") String buyOrder,
                         @RequestParam("child_buy_order") String childBuyOrder,
                         @RequestParam("child_commerce_code") String childCommerceCode,
                         @RequestParam double amount,
            Model model)
            throws IOException, TransactionRefundException {
        addPageMetadata(model, NAV_TWO_STEP, "Reembolso");
        var resp = getTransaction().refund(buyOrder, childCommerceCode, childBuyOrder, amount);
        model.addAttribute("buy_order", buyOrder);
        model.addAttribute(MODEL_RESPONSE_JSON, toJson(resp));
        return VIEW_REFUND;
    }

    @GetMapping("/info-bin")
    public String infoBin(@RequestParam("tbk_user") String tbkUser, Model model)
            throws IOException, QueryBinException {
        addPageMetadata(model, NAV_TWO_STEP, "Consulta servicio de bines");
        var requestData = Map.of(TBK_USER, tbkUser);
        var resp = getBinInfo().queryBin(tbkUser);
        model.addAttribute(REQUEST_DATA, requestData);
        model.addAttribute(REQUEST_DATA_JSON, toJson(requestData));
        model.addAttribute(MODEL_RESPONSE_JSON, toJson(resp));
        return VIEW_INFO_BIN;
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("Error inesperado", e);
        model.addAttribute("error", getDisplayableErrorMessage(e));
        return VIEW_ERROR;
    }

    private void addPageMetadata(Model model, Map<String, String> navigation, String label) {
        addNavigation(model, navigation);
        addBreadcrumbs(model, label, "#");
    }

    private void addNavigation(Model model, Map<String, String> navigation) {
        model.addAttribute(MODEL_NAVIGATION, navigation);
    }

    private void addBreadcrumbs(Model model, String label, String url) {
        Map<String, String> breadcrumbs = new LinkedHashMap<>();
        breadcrumbs.put("Inicio", "/");
        breadcrumbs.put(PRODUCT, BASE_URL);
        if (label != null) breadcrumbs.put(label, url);
        model.addAttribute("product", PRODUCT);
        model.addAttribute("base_url", BASE_URL);
        model.addAttribute("breadcrumbs", breadcrumbs);
    }

    private Oneclick.MallInscription getInscription() {
        return new Oneclick.MallInscription(getOptions());
    }

    private Oneclick.MallTransaction getTransaction() {
        return new Oneclick.MallTransaction(getOptions());
    }

    private Oneclick.MallBinInfo getBinInfo() {
        return new Oneclick.MallBinInfo(getOptions());
    }

    private WebpayOptions getOptions() {
        return new WebpayOptions(
                getConfiguredValue(commerceCode, ENV_COMMERCE_CODE),
                getConfiguredValue(apiKey, ENV_API_KEY),
                IntegrationType.TEST
        );
    }

    private String getConfiguredValue(String propertyValue, String envName) {
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        return getEnv(envName);
    }

    private String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = DOTENV.get(name);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("La variable de entorno " + name + " es obligatoria.");
        }
        return value;
    }

    private static Map<String, String> loadDotenv() {
        Path path = Path.of(".env");
        if (!Files.exists(path)) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                String[] parts = trimmed.split("=", 2);
                values.put(parts[0].trim(), parts[1].trim());
            }
        } catch (IOException e) {
            return Map.of();
        }
        return values;
    }
}
