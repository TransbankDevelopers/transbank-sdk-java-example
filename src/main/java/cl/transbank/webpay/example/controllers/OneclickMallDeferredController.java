package cl.transbank.webpay.example.controllers;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.exception.*;
import cl.transbank.webpay.oneclick.Oneclick;
import cl.transbank.webpay.oneclick.model.MallTransactionCreateDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/oneclick-mall-diferido")
public class OneclickMallDeferredController extends BaseController {

    private static final String TEMPLATE_FOLDER = "oneclick_mall_deferred";
    private static final String BASE_URL = "/oneclick-mall-deferred";
    private static final String PRODUCT = "Oneclick Mall Diferido";

    private static final String VIEW_START = TEMPLATE_FOLDER + "/start";
    private static final String VIEW_FINISH = TEMPLATE_FOLDER + "/finish";
    private static final String VIEW_AUTHORIZE = TEMPLATE_FOLDER + "/authorize";
    private static final String VIEW_STATUS = TEMPLATE_FOLDER + "/status";
    private static final String VIEW_REFUND = TEMPLATE_FOLDER + "/refund";
    private static final String VIEW_CAPTURE = TEMPLATE_FOLDER + "/capture";
    private static final String VIEW_DELETE = TEMPLATE_FOLDER + "/delete";

    private final Oneclick.MallInscription inscription;
    private final Oneclick.MallTransaction transaction;


    
    private static final Map<String, String> NAV_START;
    private static final Map<String, String> NAV_FINISH;
    private static final Map<String, String> NAV_DELETE;
    private static final Map<String, String> NAV_AUTHORIZE;
    private static final Map<String, String> NAV_STATUS;
    private static final Map<String, String> NAV_CAPTURE;
    private static final Map<String, String> NAV_REFUND;

    static {
        NAV_START = new LinkedHashMap<>();
        NAV_START.put("request", "Petición");
        NAV_START.put("response", "Respuesta");
        NAV_START.put("form", "Formulario");
        NAV_START.put("example", "Ejemplo");

        NAV_FINISH = new LinkedHashMap<>();
        NAV_FINISH.put("data", "Datos");
        NAV_FINISH.put("response", "Petición");
        NAV_FINISH.put("response", "Respuesta");
        NAV_FINISH.put("authorize", "Autorizar una transacción");

        NAV_DELETE = new LinkedHashMap<>();
        NAV_DELETE.put("data", "Borrar usuario");

        NAV_AUTHORIZE = new LinkedHashMap<>();
        NAV_AUTHORIZE.put("request", "Petición");
        NAV_AUTHORIZE.put("response", "Respuesta");
        NAV_AUTHORIZE.put("other", "Otras utilidades");

        NAV_CAPTURE = NAV_AUTHORIZE;

        NAV_STATUS = new LinkedHashMap<>();
        NAV_STATUS.put("request", "Petición");
        NAV_STATUS.put("response", "Respuesta");

        NAV_REFUND = NAV_STATUS;
    }

    public OneclickMallDeferredController() {
        var options = new WebpayOptions(
                IntegrationCommerceCodes.ONECLICK_MALL_DEFERRED,
                IntegrationApiKeys.WEBPAY,
                IntegrationType.TEST
        );
        inscription = new Oneclick.MallInscription(options);
        transaction = new Oneclick.MallTransaction(options);
    }

    private void addBreadcrumbs(Model model, String label, String url) {
        Map<String, String> breadcrumbs = new LinkedHashMap<>();
        breadcrumbs.put("Inicio", "/");
        breadcrumbs.put(PRODUCT, BASE_URL + "/start");
        if (label != null) breadcrumbs.put(label, url);
        model.addAttribute("product", PRODUCT);
        model.addAttribute("breadcrumbs", breadcrumbs);
    }

    @GetMapping("/start")
    public String start(HttpServletRequest req, Model model)
            throws IOException, InscriptionStartException {

        model.addAttribute("navigation", NAV_START);
        addBreadcrumbs(model, "Iniciar inscripción", "#");

        String username = "user_" + getRandomNumber();
        String email = "user." + getRandomNumber() + "@example.com";
        String returnUrl = req.getRequestURL().toString().replace("start", "finish");

        var resp = inscription.start(username, email, returnUrl);

        model.addAttribute("request_data", Map.of(
                "username", username,
                "email", email,
                "returnUrl", returnUrl
        ));
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));
        model.addAttribute("url", resp.getUrlWebpay());
        model.addAttribute("token", resp.getToken());

        req.getSession().setAttribute("username", username);
        req.getSession().setAttribute("email", email);

        return VIEW_START;
    }

    @GetMapping("/finish")
    public String finish(HttpServletRequest req,
                         @RequestParam(name = "TBK_TOKEN", required = false) String token,
                         Model model)
            throws IOException, InscriptionFinishException {

        model.addAttribute("navigation", NAV_FINISH);        
        addBreadcrumbs(model, "Finalizar inscripción", "#");

        String username = (String) req.getSession().getAttribute("username");

        var resp = inscription.finish(token);

        req.getSession().setAttribute("tbkUser", resp.getTbkUser());

        model.addAttribute("request_data", Map.of(
                "username", username,
                "tbkUser", resp.getTbkUser()
        ));

        model.addAttribute("token", token);
        model.addAttribute("username", username);
        model.addAttribute("tbk_user", resp.getTbkUser());
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));

        model.addAttribute("child_commerce_code1", IntegrationCommerceCodes.ONECLICK_MALL_DEFERRED_CHILD1);
        model.addAttribute("child_commerce_code2", IntegrationCommerceCodes.ONECLICK_MALL_DEFERRED_CHILD2);

        return VIEW_FINISH;
    }

    @GetMapping("/delete")
    public String delete(@RequestParam String username,
                         @RequestParam("tbk_user") String tbkUser,
                         Model model)
            throws IOException, InscriptionDeleteException {

        model.addAttribute("navigation", NAV_DELETE); 
        addBreadcrumbs(model, "Eliminar inscripción", "#");

        inscription.delete(tbkUser, username);
        model.addAttribute("message", "Inscripción eliminada exitosamente.");

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

        model.addAttribute("navigation", NAV_AUTHORIZE); 
        addBreadcrumbs(model, "Autorizar transacción", "#");

        String buyOrder = "buyOrder_" + getRandomNumber();
        String childBuyOrder1 = "childOrder1_" + getRandomNumber();
        String childBuyOrder2 = "childOrder2_" + getRandomNumber();

        var details = MallTransactionCreateDetails
                .build()
                .add(
                        amount1,
                        childCode1,
                        childBuyOrder1,
                        (byte) installments1
                )
                .add(
                        amount2,
                        childCode2,
                        childBuyOrder2,
                        (byte) installments2
                );

        var resp = transaction.authorize(username, tbkUser, buyOrder, details);

        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_AUTHORIZE;
    }

    @GetMapping("/status")
    public String status(@RequestParam("buy_order") String buyOrder, Model model)
            throws IOException, TransactionStatusException {
        
        model.addAttribute("navigation", NAV_STATUS);        
        addBreadcrumbs(model, "Consultar estado", "#");

        var resp = transaction.status(buyOrder);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_STATUS;
    }

    @GetMapping("/refund")
    public String refund(@RequestParam("buy_order") String buyOrder,
                         @RequestParam("child_buy_order") String childBuyOrder,
                         @RequestParam("child_commerce_code") String childCommerceCode,
                         @RequestParam double amount,
                         Model model)
            throws IOException, TransactionRefundException {

        model.addAttribute("navigation", NAV_REFUND);        
        addBreadcrumbs(model, "Reembolso", "#");

        model.addAttribute("buy_order", buyOrder);
        var resp = transaction.refund(buyOrder, childCommerceCode, childBuyOrder, amount);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_REFUND;
    }

    @GetMapping("/capture")
    public String capture(
            @RequestParam("buy_order") String buyOrder,
            @RequestParam("child_commerce_code") String childCommerceCode,
            @RequestParam("child_buy_order") String childBuyOrder,
            @RequestParam("authorization_code") String authorizationCode,
            @RequestParam double amount,
            Model model)
            throws IOException, TransactionCaptureException {

        model.addAttribute("navigation", NAV_CAPTURE);        
        addBreadcrumbs(model, "Capturar", "#");

        model.addAttribute("buy_order", buyOrder);
        model.addAttribute("child_buy_order", childBuyOrder);
        model.addAttribute("child_commerce_code", childCommerceCode);

        var resp = transaction.capture(childCommerceCode, childBuyOrder, authorizationCode, amount);
        model.addAttribute("response_data", resp);
        model.addAttribute("response_data_json", toJson(resp));

        return VIEW_CAPTURE;
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("Error inesperado", e);
        model.addAttribute("error", e.getMessage());
        return VIEW_ERROR;
    }
}
