package com.sahay.third.party.modules;

import com.sahay.third.party.model.AirtimePayment;
import com.sahay.third.party.repo.AirtimePaymentRepository;
import com.sahay.third.party.service.GlobalMethods;
import com.sahay.third.party.service.HttpProcessor;
import com.sahay.third.party.service.SmsLogging;
import lombok.extern.java.Log;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class AccountBalance {

    @Value(value = "${org.app.properties.paybill.payment.otp}")
    private String TEMPLATE_ID;

    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;


    private final SmsLogging smsLogging;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;
    private final AirtimePaymentRepository airtimePaymentRepository;
    private final ProcessingService processingService;

    @Autowired
    public AccountBalance(SmsLogging smsLogging,
                          GlobalMethods globalMethods,
                          HttpProcessor httpProcessor,
                          AirtimePaymentRepository airtimePaymentRepository,
                          ProcessingService processingService) {
        this.smsLogging = smsLogging;
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
        this.airtimePaymentRepository = airtimePaymentRepository;
        this.processingService = processingService;
    }

    public JSONObject pickAndProcess(JSONObject jsonObject) {
        JSONObject response = new JSONObject();
        try {
            JSONObject accBalance = new JSONObject();
            accBalance.put("TransactionReqType", "BILL-ACC-BALANCE");
            accBalance.put("SettlementAccountNumber", jsonObject.getString("SettlementAccount"));
            RequestBuilder airBuilder = new RequestBuilder("POST");
            airBuilder.addHeader("Content-Type", "application/json")
                    .setBody(accBalance.toString())
                    .setUrl(SP_URL)
                    .build();

            JSONObject jsonResponse = httpProcessor.processProperRequest(airBuilder);
            if (jsonResponse.getString("Status").equals("00")) {
                response.put("FloatBalance", jsonResponse.getDouble("FloatBalance"));
                response.put("SettlementBalance", jsonResponse.getDouble("SettlementBalance"));
                response.put("CommissionBalance", jsonResponse.getDouble("CommissionBalance"));
                response.put("FloatAccount", jsonResponse.getString("FloatAccount"));
                response.put("response", "000");
                response.put("responseDescription", "Account Balance request was successful");
            } else {
                response.put("FloatBalance", 0.00);
                response.put("SettlementBalance", 0.00);
                response.put("CommissionBalance", 0.00);
                response.put("FloatAccount", jsonObject.getString("SettlementAccount"));
                response.put("response", "001");
                response.put("responseDescription", "Account Balance request was not successful");
            }
        } catch (Exception ex) {
            log.log(Level.WARNING, "Bill Account Balance Error : " + ex.getMessage());
            response.put("FloatBalance", 0.00);
            response.put("SettlementBalance", 0.00);
            response.put("CommissionBalance", 0.00);
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;
    }
}
