package cl.transbank.webpay.example.controllers;

import com.google.gson.GsonBuilder;
import java.util.Random;

public  abstract class BaseController {

    public String toJson(Object obj){
        return (new GsonBuilder().setPrettyPrinting().create()).toJson(obj);
    }

    protected String getRandomNumber(){
        return String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
    }
}
