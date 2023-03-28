package com.sahay.third.party.modules.transfer;

import com.sahay.third.party.service.GlobalMethods;
import com.sahay.third.party.service.HttpProcessor;
import com.sahay.third.party.service.SmsLogging;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class EthioSwitchService {
    @Value(value = "${org.app.properties.paybill.payment.otp}")
    private String TEMPLATE_ID;

    @Value(value = "${org.app.properties.ethio.swtich.banks.endpoint}")
    private String ETHIO_SWTICH_BANKS;

    @Value(value = "${org.app.properties.ethio.swtich.endpoint}")
    private String ETHIO_SWTICH_ENDPOINT;

    private final SmsLogging smsLogging;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;

    @Autowired
    public EthioSwitchService(SmsLogging smsLogging,
                              GlobalMethods globalMethods,
                              HttpProcessor httpProcessor) {
        this.smsLogging = smsLogging;
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
    }

    public JSONObject availableBanks() {
        JSONObject response = new JSONObject();
        try {
            RequestBuilder transBuilder = new RequestBuilder("GET");
            transBuilder.addHeader("Content-Type", "application/json")
                    .setUrl(ETHIO_SWTICH_BANKS)
                    .build();
            JSONObject transRes = httpProcessor.jsonRequestProcessor(transBuilder);
            response.put("banksBdy", new JSONArray(transRes.getString("ResponseBody")));
            response.put("response", "00");
            response.put("responseDescription", "Success");
        } catch (Exception ex) {
            log.log(Level.WARNING, "Ethio Switch Get Banks - Error : " + ex.getMessage());
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;
    }


    public JSONObject accountCheck(JSONObject jsonObject) {
        JSONObject resp = new JSONObject();
        try {
            JSONObject transPosting = new JSONObject();
            transPosting.put("TransactionType", "ACC-LCK");
            transPosting.put("InstId", jsonObject.getString("InstId"));
            transPosting.put("AccountFrom", jsonObject.getString("SettlementAccount"));
            transPosting.put("CustomerType", "5002");
            transPosting.put("AccountNumber", jsonObject.getString("Account"));

            RequestBuilder transBuilder = new RequestBuilder("POST");
            transBuilder.addHeader("Content-Type", "application/json")
                    .setBody(transPosting.toString())
                    .setUrl(ETHIO_SWTICH_ENDPOINT)
                    .build();

            resp = httpProcessor.processProperRequest(transBuilder);
        } catch (Exception ex) {
            resp.put("response", "999");
            resp.put("responseDescription", "Error in Transaction Processing");
        }
        return resp;
    }

    public JSONObject fundsTransfer(JSONObject jsonObject) {
        JSONObject response = new JSONObject();
        try {

            JSONObject transPosting = new JSONObject();
            transPosting.put("TransactionType", "FTTES");
            transPosting.put("FromType", "SAHAY");
            transPosting.put("InstId", jsonObject.getString("InstId"));
            transPosting.put("CustomerType", "5002");
            transPosting.put("AccountFrom", jsonObject.getString("FloatAccount"));
            transPosting.put("AccountNumber", jsonObject.getString("AccountNumber"));
            transPosting.put("amount", jsonObject.getString("Amount"));
            transPosting.put("AccountName", jsonObject.getString("AccountName"));
            transPosting.put("msisdn", jsonObject.getString("PhoneNumber"));
            transPosting.put("ReceiverNumber", jsonObject.getString("ReceiverNumber"));
            transPosting.put("ReceiverName", jsonObject.getString("ReceiverName"));

            response.put("TransactionPostPayload", transPosting.toString());
            response.put("ThirdPartyPayload", transPosting.toString());

            RequestBuilder transBuilder = new RequestBuilder("POST");
            transBuilder.addHeader("Content-Type", "application/json")
                    .setBody(transPosting.toString())
                    .setUrl(ETHIO_SWTICH_ENDPOINT)
                    .build();
            JSONObject jsonTransResponse = httpProcessor.processProperRequest(transBuilder);

            response.put("ThirdPartyResponse", jsonTransResponse.toString());

            if (jsonTransResponse.getString("response").equals("000")) {
                response.put("response", "000");
                response.put("responseDescription", "Funds Transfer was successful");
                response.put("TransRef", jsonTransResponse.getString("refNumber"));
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
                response.put("responseDescription", "Failed - " + jsonTransResponse.getString("responseDescription"));
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "Ethio Switch Transfer Error : " + ex.getMessage());
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;
    }
}
