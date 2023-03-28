package com.sahay.third.party.modules.transfer;

import com.sahay.third.party.service.GlobalMethods;
import com.sahay.third.party.service.HttpProcessor;
import com.sahay.third.party.service.SmsLogging;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;


@Log
@Component
@SuppressWarnings("Duplicates")
public class SahayService {
    @Value(value = "${org.app.properties.paybill.payment.otp}")
    private String TEMPLATE_ID;

    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;

    private final SmsLogging smsLogging;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;

    public SahayService(SmsLogging smsLogging,
                        GlobalMethods globalMethods,
                        HttpProcessor httpProcessor) {
        this.smsLogging = smsLogging;
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
    }

    public JSONObject pickAndProcess(JSONObject jsonObject) {
        JSONObject response = new JSONObject();
        try {
            JSONObject transPosting = new JSONObject();
            transPosting.put("TransactionReqType", "BILL-FT");
            transPosting.put("ClientReqType", "BT");
            transPosting.put("TransactionId", jsonObject.getString("TransRef"));
            transPosting.put("SettlementAccount", jsonObject.getString("SettlementAccount"));
            transPosting.put("FromAccount", jsonObject.getString("FloatAccount"));
            transPosting.put("ToAccount", jsonObject.getString("AccountNumber"));
            transPosting.put("Amount", jsonObject.getString("Amount"));
            transPosting.put("Narration", "Funds Settlement to Sahay Account : " + jsonObject.getString("AccountNumber"));
            transPosting.put("Channel", "API");

            response.put("TransactionPostPayload", transPosting.toString());
            response.put("ThirdPartyPayload", transPosting.toString());

            RequestBuilder transBuilder = new RequestBuilder("POST");
            transBuilder.addHeader("Content-Type", "application/json")
                    .setBody(transPosting.toString())
                    .setUrl(SP_URL)
                    .build();
            JSONObject jsonTransResponse = httpProcessor.processProperRequest(transBuilder);
            response.put("ThirdPartyResponse", jsonTransResponse.toString());

            if (jsonTransResponse.getString("Status").equals("00")) {
                if (jsonTransResponse.getString("EnoughBalance").equals("0")) {
                    response.put("response", "000");
                    response.put("responseDescription", "Funds Transfer was successful");
                    response.put("sahayRef", jsonObject.getString("TransRef"));
                    JSONObject jsonObj = new JSONObject();
                    Date date = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                    String strDate = dateFormat.format(date);
                    String strTime = timeFormat.format(date);
                    jsonObj.put("TemplateId", TEMPLATE_ID);
                    jsonObj.put("Phone", jsonObject.getString("PhoneNumber"));
                    jsonObj.put("name", globalMethods.getSahayName(jsonObject.getString("PhoneNumber")));
                    jsonObj.put("amount", jsonObject.getString("Amount"));
                    jsonObj.put("biller_name", jsonObject.getString("ClientName"));
                    jsonObj.put("date", strDate);
                    jsonObj.put("time", strTime);

                    String[] words = {"name", "amount", "biller_name", "date", "time", "otp"};
                    String message = smsLogging.generateMessage(jsonObj, words);
                    globalMethods.sendSMS(message, jsonObject.getString("PhoneNumber"), jsonObject.getString("TransRef"));
                } else {
                    response.put("response", "999");
                    response.put("responseDescription", "Insufficient Balance");
                }
            } else {
                response.put("response", "999");
                response.put("responseDescription", "Error in Transaction Payment failed");
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "Sahay Funds Transfer Error : " + ex.getMessage());
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;
    }
}
