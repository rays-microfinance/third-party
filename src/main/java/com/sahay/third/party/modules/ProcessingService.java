package com.sahay.third.party.modules;


import com.sahay.third.party.service.HttpProcessor;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProcessingService {

    @Value(value = "${org.app.properties.gateway}")
    private String URL_CHECKOUT;

    private final HttpProcessor httpProcessor;

    @Autowired
    public ProcessingService(HttpProcessor httpProcessor) {
        this.httpProcessor = httpProcessor;
    }

    public JSONObject pickAndProcess(JSONObject request) {

        request = getRequestPayload(request);
        RequestBuilder builder = new RequestBuilder("POST");
        builder.addHeader("Content-Type", "application/json")
                .setBody(request.toString())
                .setUrl(URL_CHECKOUT)
                .build();
        return httpProcessor.processProperRequest(builder);
    }

    private JSONObject defaultLoad() {
        JSONObject load = new JSONObject();
        load.put("username", "channel");
        load.put("password", "$_@C0NNEKT");
        load.put("messageType", "1200");
        load.put("transactionId", "1200560161");
        load.put("timestamp", "20200101120000");
        load.put("channel", "USSD");
        return load;
    }

    public JSONObject getRequestPayload(JSONObject data) {
        JSONObject load = new JSONObject();
        switch (data.getString("transactionType")) {
            case "CUD":
                load = getAccountRequest(data);
                break;
            case "LOG":
                load = getLoginRequest(data);
                break;
            case "BAI":
                load = getBalance(data);
                break;
            case "FT":
                load = getFundsTransfer(data);
                break;
            case "NAM":
                load = getAgentMerchantName(data);
                break;
        }
        return load;
    }

    private JSONObject getAccountRequest(JSONObject data) {
        JSONObject load = defaultLoad();
        load.put("serviceCode", "100");
        load.put("transactionType", "CUD");
        load.put("msisdn", data.getString("msisdn"));
        return load;
    }

    private JSONObject getLoginRequest(JSONObject data) {
        JSONObject load = defaultLoad();
        load.put("serviceCode", "130");
        load.put("transactionType", "LOG");
        load.put("pin", data.getString("pin"));
        load.put("customerType", "1");
        load.put("msisdn", data.getString("msisdn"));
        return load;
    }

    private JSONObject getBalance(JSONObject data) {
        JSONObject load = defaultLoad();
        load.put("serviceCode", "150");
        load.put("transactionType", "BAI");
        load.put("msisdn", data.getString("msisdn"));
        load.put("accountNumber", data.getString("accountNumber"));
        return load;
    }

    private JSONObject getFundsTransfer(JSONObject data) {
        JSONObject load = defaultLoad();
        load.put("serviceCode", "200");
        load.put("transactionType", "FT");
        load.put("msisdn", data.getString("msisdn"));
        load.put("fromAccount", data.getString("accountNumber"));
        load.put("toAccount", data.getString("toAccount"));
        load.put("amount", data.getString("amount"));
        return load;
    }

    private JSONObject getAgentMerchantName(JSONObject data) {
        JSONObject load = defaultLoad();
        load.put("serviceCode", "180");
        load.put("transactionType", "NAM");
        load.put("detType", data.getString("detType"));
        load.put("accountNumber", data.getString("accountNumber"));
        return load;
    }


}
