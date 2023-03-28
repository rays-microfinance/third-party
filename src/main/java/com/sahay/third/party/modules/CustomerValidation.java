package com.sahay.third.party.modules;


import com.sahay.third.party.service.GlobalMethods;
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
public class CustomerValidation {

    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;

    private final HttpProcessor httpProcessor;
    private final ProcessingService processingService;

    @Autowired
    public CustomerValidation(HttpProcessor httpProcessor,
                              ProcessingService processingService) {
        this.httpProcessor = httpProcessor;
        this.processingService = processingService;
    }

    public JSONObject pickAndProcess(JSONObject requestObject) {
        JSONObject response = requestObject;
        try {
            JSONObject load = new JSONObject();
            if (!requestObject.getString("PhoneNumber").equals("")) {
                load.put("msisdn", requestObject.getString("PhoneNumber"));
                load.put("transactionType", "CUD");
                JSONObject cudResponse = processingService.pickAndProcess(load);
                JSONObject detail = cudResponse.getJSONObject("detail");
                //check validation if the customer exists
                if (detail.has("LinkedAccounts")) {
                    response.put("customerName", detail.getString("FirstName") + " " + detail.getString("LastName") + " " + detail.getString("GrandFatherName"));
                    response.put("response", "000");
                    response.put("responseDescription", "The Customer exists");
                } else {
                    response.put("response", "999");
                    response.put("responseDescription", "The Phone number is not registered with Sahay");
                }
            } else {
                load.put("TransactionReqType", "CHECK-ACCOUNT");
                String type = "Agent";
                if (requestObject.getString("customerType").equals("AGT")) {
                    load.put("AccountType", "200");
                } else {
                    type = "Merchant";
                    load.put("AccountType", "300");
                }
                load.put("Account", requestObject.getString("account"));
                RequestBuilder builder = new RequestBuilder("POST");
                builder.addHeader("Content-Type", "application/json")
                        .setBody(load.toString())
                        .setUrl(SP_URL)
                        .build();
                JSONObject jsonResponse = httpProcessor.processProperRequest(builder);
                if (jsonResponse.getString("CustomerAccountExist").equals("1")) {
                    response.put("customerMsisdn", jsonResponse.getString("CustomerMsisdn"));
                    response.put("customerName", jsonResponse.getString("CustomerName"));
                    response.put("response", "000");
                    response.put("responseDescription", "The " + type + " exists");
                } else {
                    response.put("response", "999");
                    response.put("responseDescription", "The " + type + " code is not registered with Sahay");
                }
            }
        } catch (
                Exception ex) {
            log.log(Level.WARNING, "CUSTOMER CLASS : " + ex.getMessage());
            response.put("response", "101");
            response.put("responseDescription", "Failed while processing the request");
        }
        return response;
    }
}
