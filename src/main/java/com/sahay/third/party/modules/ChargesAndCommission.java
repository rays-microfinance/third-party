package com.sahay.third.party.modules;


import com.sahay.third.party.service.HttpProcessor;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.logging.Level;

@Log
@Service
@SuppressWarnings("Duplicates")
public class ChargesAndCommission {
    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;

    private final HttpProcessor httpProcessor;


    @Autowired
    public ChargesAndCommission(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;

    }

    public JSONObject pickAndProcess(JSONObject requestObject) {
        JSONObject response = requestObject;
        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("TransactionReqType", "CHARGES");
            jsonRequest.put("TransactionType", requestObject.getString("transactionType"));
            jsonRequest.put("Amount", requestObject.getString("amount"));
            RequestBuilder builder = new RequestBuilder("POST");
            builder.addHeader("Content-Type", "application/json")
                    .setBody(jsonRequest.toString())
                    .setUrl(SP_URL)
                    .build();
            JSONObject jsonResponse = httpProcessor.processProperRequest(builder);
            response.put("transactionCost", jsonResponse.getString("TransactionCost"));
            response.put("customerCharge", jsonResponse.getString("CustomerCharge"));
            response.put("awashEarnedCommission", jsonResponse.getString("AwashTransactionCommission"));
            response.put("sahayEarnedCommission", jsonResponse.getString("BankTransactionCommission"));
            response.put("response", "000");
            response.put("responseDescription", "Charges Look up successful");
        } catch (
                Exception ex) {
            log.log(Level.WARNING, "CHARGES AND COMMISSION CLASS : " + ex.getMessage());
            response.put("response", "101");
            response.put("responseDescription", "Failed while processing the request");
        }
        return response;
    }


}
