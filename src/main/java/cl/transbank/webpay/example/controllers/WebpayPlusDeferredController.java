package cl.transbank.webpay.example.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Log4j2
@Controller
@RequestMapping("/webpay_plus_deferred")
public class WebpayPlusDeferredController extends BaseController {

    @GetMapping("/create")
    public String createPage(HttpServletRequest req, Model model) {
        // TODO: Implement logic
        return "webpay_plus_deferred/create";
    }

}
