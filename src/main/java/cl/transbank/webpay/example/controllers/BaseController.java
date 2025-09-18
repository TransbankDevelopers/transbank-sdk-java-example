package cl.transbank.webpay.example.controllers;

import com.google.gson.GsonBuilder;

import java.util.Random;

public abstract class BaseController {

    protected static final String VIEW_ERROR = "error/error_page";
    protected static final String VIEW_ABORTED_ERROR = "error/aborted";
    protected static final String VIEW_FORM_ERROR = "error/form_error";
    protected static final String VIEW_TIMEOUT_ERROR = "error/timeout";

    public String toJson(Object obj) {
        return (new GsonBuilder().setPrettyPrinting().create()).toJson(obj);
    }

    protected String getRandomNumber() {
        return String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
    }
}
