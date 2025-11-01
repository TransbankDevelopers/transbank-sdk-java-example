package cl.transbank.webpay.example.controllers;

import com.google.gson.GsonBuilder;

import java.util.Random;

public abstract class BaseController {

    protected static final String VIEW_ERROR = "error/error_page";
    protected static final String VIEW_ABORTED_ERROR = "error/webpay/aborted";
    protected static final String VIEW_FORM_ERROR = "error/webpay/form_error";
    protected static final String VIEW_TIMEOUT_ERROR = "error/webpay/timeout";
    protected static final String VIEW_RECOVER_ERROR = "error/oneclick/recover";
    protected static final String VIEW_REJECTED_ERROR = "error/oneclick/rejected";

    public String toJson(Object obj) {
        return (new GsonBuilder().setPrettyPrinting().create()).toJson(obj);
    }

    protected String getRandomNumber() {
        return String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
    }
}
