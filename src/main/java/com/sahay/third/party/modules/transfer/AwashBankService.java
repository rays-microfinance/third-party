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
import java.time.Instant;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class AwashBankService {
    @Value(value = "${org.app.properties.awash.endpoint}")
    private String AWASH_URL;

    @Value(value = "${org.app.properties.paybill.payment.otp}")
    private String TEMPLATE_ID;

    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;

    private final SmsLogging smsLogging;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;

    public AwashBankService(SmsLogging smsLogging,
                            GlobalMethods globalMethods,
                            HttpProcessor httpProcessor) {
        this.smsLogging = smsLogging;
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
    }

    public JSONObject pickAndProcess(JSONObject jsonObject) {
        JSONObject response = new JSONObject();
        try {
            JSONObject accLookUp = new JSONObject();
            accLookUp.put("transactionType", "ACC-LCK");
            accLookUp.put("msisdn", jsonObject.getString("PhoneNumber"));
            accLookUp.put("accountNumber", jsonObject.getString("AccountNumber"));
            RequestBuilder availBuilder = new RequestBuilder("POST");
            availBuilder.addHeader("Content-Type", "application/json")
                    .setBody(accLookUp.toString())
                    .setUrl(SP_URL)
                    .build();
            JSONObject acLookRes = httpProcessor.processProperRequest(availBuilder);

            response.put("TransactionPostPayload", accLookUp.toString());
            response.put("ThirdPartyPayload", accLookUp.toString());

            if (acLookRes.getString("response").equals("999")) {
                response.put("response", "999");
                response.put("responseDescription", "The Account is not available");
            } else {
                JSONObject transfer = new JSONObject();
                transfer.put("TransactionReqType", "BTAW");
                transfer.put("ClientReqType", jsonObject.getString("Client"));
                transfer.put("TransactionId", jsonObject.getString("TransRef"));
                transfer.put("BillAccount", jsonObject.getString("AccountNumber"));
                transfer.put("ToAccount", jsonObject.getString("SettlementAccount"));
                transfer.put("PhoneNumber", jsonObject.getString("PhoneNumber"));
                transfer.put("Amount", jsonObject.getString("Amount"));
                transfer.put("Narration", "Driver Account Transfer Ref: " + jsonObject.getString("BillerReference"));
                transfer.put("Channel", "API");
                RequestBuilder awsTransferBuilder = new RequestBuilder("POST");
                response.put("ThirdPartyPayload", transfer.toString());
                awsTransferBuilder.addHeader("Content-Type", "application/json")
                        .setBody(transfer.toString())
                        .setUrl(AWASH_URL)
                        .build();
                JSONObject awsTransferRes = httpProcessor.processProperRequest(awsTransferBuilder);

                if (awsTransferRes.getString("response").equals("000")) {
                    response.put("response", "000");
                    response.put("responseDescription", "successful");
                } else {
                    response.put("response", "999");
                    response.put("responseDescription", awsTransferRes.getString("responseDescription"));
                }
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "Awash Funds Transfer Error : " + ex.getMessage());
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;

    }
}
