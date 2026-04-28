package cl.transbank.webpay.example.controllers;

import cl.transbank.exception.TransbankException;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.math.BigDecimal;
import java.util.Random;

public abstract class BaseController {
    private static final String GENERIC_ERROR_MESSAGE =
            "Ocurrió un error inesperado al procesar la operación.";

    protected static final String VIEW_ERROR = "error/error_page";
    protected static final String VIEW_ABORTED_ERROR = "error/webpay/aborted";
    protected static final String VIEW_FORM_ERROR = "error/webpay/form_error";
    protected static final String VIEW_TIMEOUT_ERROR = "error/webpay/timeout";
    protected static final String VIEW_RECOVER_ERROR = "error/oneclick/recover";
    protected static final String VIEW_REJECTED_ERROR = "error/oneclick/rejected";

    private static final JsonSerializer<Double> DOUBLE_SERIALIZER = (value, type, ctx) -> {
        if (value.isNaN() || value.isInfinite()) {
            return new JsonPrimitive(value);
        }
        return new JsonPrimitive(new BigDecimal(value.toString()).stripTrailingZeros().toPlainString());
    };

    public String toJson(Object obj) {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Double.class, DOUBLE_SERIALIZER)
                .registerTypeAdapter(double.class, DOUBLE_SERIALIZER)
                .create()
                .toJson(obj);
    }

    protected String getRandomNumber() {
        return String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
    }

    protected String getDisplayableErrorMessage(Exception e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof TransbankException && current.getMessage() != null && !current.getMessage().isBlank()) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return GENERIC_ERROR_MESSAGE;
    }
}
