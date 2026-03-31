package cl.transbank.webpay.example.models;

import java.io.Serializable;

public class MallDetailSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double amount;
    private final String commerceCode;
    private final String buyOrder;

    public MallDetailSession(double amount, String commerceCode, String buyOrder) {
        this.amount = amount;
        this.commerceCode = commerceCode;
        this.buyOrder = buyOrder;
    }

    public double getAmount() {
        return amount;
    }

    public String getCommerceCode() {
        return commerceCode;
    }

    public String getBuyOrder() {
        return buyOrder;
    }
}
