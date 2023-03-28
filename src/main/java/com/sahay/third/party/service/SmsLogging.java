package com.sahay.third.party.service;


import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class SmsLogging {

    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;

    private final HttpProcessor httpProcessor;

    @Autowired
    public SmsLogging(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    public String generateMessage(JSONObject data, String[] words) {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("TransactionReqType", "SMS-TEMPLATE");
        jsonRequest.put("TemplateId", data.getString("TemplateId"));
        jsonRequest.put("TemplatePhone", data.getString("Phone"));

        RequestBuilder payBuilder = new RequestBuilder("POST");
        payBuilder.addHeader("Content-Type", "application/json")
                .setBody(jsonRequest.toString())
                .setUrl(SP_URL)
                .build();
        JSONObject payResponse = httpProcessor.processProperRequest(payBuilder);

        AtomicReference<String> message = new AtomicReference<>(payResponse.getString("Template"));
        try {
            for (String word : words)
                try {
                    message.set(message.get().replace("[" + word + "?]", data.getString(word)));
                } catch (Exception ex) {
                    log.log(Level.WARNING, "Error Mapping SMS : " + ex.getMessage());
                }
        } catch (Exception ex) {
            log.log(Level.WARNING, "Error Generating SMS : " + ex.getMessage());
        }
        return message.get();
    }
}
