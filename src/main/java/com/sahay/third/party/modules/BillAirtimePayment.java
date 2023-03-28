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
import java.util.IllegalFormatCodePointException;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class BillAirtimePayment {

    @Value(value = "${org.app.properties.paybill.payment.otp}")
    private String TEMPLATE_ID;

    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;

    @Value(value = "${org.app.properties.airtime.endpoint}")
    private String AIRTIME_URL;

    private final SmsLogging smsLogging;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;
    private final AccountBalance accountBalance;
    private final AirtimePaymentRepository airtimePaymentRepository;

    @Autowired
    public BillAirtimePayment(SmsLogging smsLogging,
                              GlobalMethods globalMethods,
                              HttpProcessor httpProcessor,
                              AccountBalance accountBalance,
                              AirtimePaymentRepository airtimePaymentRepository) {
        this.smsLogging = smsLogging;
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
        this.accountBalance = accountBalance;
        this.airtimePaymentRepository = airtimePaymentRepository;
    }

    public JSONObject pickAndProcess(JSONObject jsonObject) {
        JSONObject response = new JSONObject();
        try {
            airtimePaymentRepository.findAirtimePaymentByBillerReference(jsonObject.getString("BillerReference"))
                    .ifPresentOrElse(payment -> {
                        response.put("response", "999");
                        response.put("responseDescription", "Transaction with the same reference exists");
                    }, () -> {
                        JSONObject accBalance = accountBalance.pickAndProcess(jsonObject);
                        if (Double.valueOf(accBalance.getString("FloatAccount")) > Double.valueOf(jsonObject.getString("Amount"))) {
                            String stagingRef = globalMethods.generateTrans();
                            AirtimePayment payment = new AirtimePayment();
                            payment.setClient(jsonObject.getString("Client"));
                            payment.setTransRef(stagingRef);
                            payment.setPhoneNumber(jsonObject.getString("PhoneNumber"));
                            payment.setBillerReference(jsonObject.getString("BillerReference"));
                            payment.setAmount(new BigDecimal(jsonObject.getString("Amount")));
                            payment.setTransactionStatus(0);
                            payment.setStatus(0);
                            payment.setCreatedDate(Timestamp.from(Instant.now()));
                            airtimePaymentRepository.save(payment);

                            JSONObject airtimePurchase = new JSONObject();
                            airtimePurchase.put("field37", stagingRef);
                            airtimePurchase.put("field65", jsonObject.getString("PhoneNumber"));
                            airtimePurchase.put("field4", jsonObject.getString("Amount"));
                            airtimePurchase.put("field98", "EXRCTRFREQ");

                            RequestBuilder airBuilder = new RequestBuilder("POST");
                            airBuilder.addHeader("Content-Type", "application/json")
                                    .setBody(airtimePurchase.toString())
                                    .setUrl(AIRTIME_URL)
                                    .build();
                            JSONObject jsonAirResponse = httpProcessor.processProperRequest(airBuilder);
                            payment.setAirtimeStatus(jsonAirResponse.getString("field39"));
                            airtimePaymentRepository.save(payment);

                            if (jsonAirResponse.getString("field39").equals("00")) {
                                JSONObject transPosting = new JSONObject();
                                transPosting.put("TransactionReqType", "BILL-AIRTIME-PAYMENT");
                                transPosting.put("ClientReqType", "ATTB");
                                transPosting.put("TransactionId", payment.getTransRef());
                                transPosting.put("ToAccount", payment.getPhoneNumber());
                                transPosting.put("BillerSettlementAccount", jsonObject.getString("SettlementAccount"));
                                transPosting.put("Amount", payment.getAmount().toString());

                                payment.setTransactionPostPayload(transPosting.toString());
                                RequestBuilder transBuilder = new RequestBuilder("POST");
                                transBuilder.addHeader("Content-Type", "application/json")
                                        .setBody(transPosting.toString())
                                        .setUrl(SP_URL)
                                        .build();
                                JSONObject jsonTransResponse = httpProcessor.processProperRequest(transBuilder);
                                log.log(Level.INFO, "ESB SERVICE RESPONSE : " + jsonTransResponse.toString());
                                if (jsonTransResponse.getString("Status").equals("00")) {
                                    response.put("FloatBalance", jsonTransResponse.getString("BillerAccountBalance"));
                                    if (jsonTransResponse.getString("EnoughBalance").equals("0")) {
                                        payment.setTransactionStatus(3);
                                        response.put("response", "000");
                                        response.put("responseDescription", "The Airtime purchase was successful");
                                        response.put("sahayRef", stagingRef);
                                        JSONObject jsonObj = new JSONObject();
                                        Date date = new Date();
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                                        String strDate = dateFormat.format(date);
                                        String strTime = timeFormat.format(date);
                                        jsonObj.put("TemplateId", TEMPLATE_ID);
                                        jsonObj.put("Phone", payment.getPhoneNumber());
                                        jsonObj.put("name", globalMethods.getSahayName(payment.getPhoneNumber()));
                                        jsonObj.put("amount", jsonObject.getString("Amount"));
                                        jsonObj.put("biller_name", jsonObject.getString("ClientName"));
                                        jsonObj.put("date", strDate);
                                        jsonObj.put("time", strTime);

                                        String[] words = {"name", "amount", "biller_name", "date", "time", "otp"};
                                        String message = smsLogging.generateMessage(jsonObj, words);
                                        globalMethods.sendSMS(message, payment.getPhoneNumber(), payment.getTransRef());
                                    } else {
                                        payment.setTransactionStatus(5);
                                        payment.setResponseDescription("Insufficient Balance");
                                        payment.setResponseStaus("999");
                                        response.put("response", "999");
                                        response.put("responseDescription", "Insufficient Balance");
                                    }
                                } else {
                                    payment.setResponseStaus("000");
                                    payment.setResponseDescription("Error in Transaction Payment failed");
                                    payment.setTransactionStatus(10);
                                    response.put("response", "999");
                                    response.put("responseDescription", "The Airtime purchase was not successful");
                                }
                                payment.setResponsePayload(response.toString());
                                payment.setProcessedDatetime(Timestamp.from(Instant.now()));
                                airtimePaymentRepository.save(payment);
                            } else {
                                payment.setTransactionStatus(5);
                                payment.setResponseDescription("Airtime Purchase issue. Try again later.");
                                payment.setResponseStaus("999");
                                response.put("response", "999");
                                response.put("responseDescription", "Airtime Purchase issue. Try again later");
                                airtimePaymentRepository.save(payment);
                            }
                        } else {
                            response.put("response", "999");
                            response.put("responseDescription", "Insufficient Balance");
                        }
                    });
        } catch (Exception ex) {
            log.log(Level.WARNING, "Airtime Payment Error : " + ex.getMessage());
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;
    }
}
