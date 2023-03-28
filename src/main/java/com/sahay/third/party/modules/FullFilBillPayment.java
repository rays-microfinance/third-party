package com.sahay.third.party.modules;

import com.sahay.third.party.repo.PaymentRepository;
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
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class FullFilBillPayment {
    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;

    @Value(value = "${org.app.properties.awash.endpoint}")
    private String AWASH_URL;

    @Value(value = "${org.app.properties.paybill.payment.success}")
    private String TEMPLATE_ID_SUCCESS;

    private final SmsLogging smsLogging;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;
    private final PaymentRepository paymentRepository;

    @Autowired
    public FullFilBillPayment(SmsLogging smsLogging,
                              GlobalMethods globalMethods,
                              HttpProcessor httpProcessor,
                              PaymentRepository paymentRepository) {
        this.smsLogging = smsLogging;
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
        this.paymentRepository = paymentRepository;
    }

    public JSONObject pickAndProcess(JSONObject jsonObject) {
        JSONObject response = new JSONObject();
        try {
            paymentRepository.findPaymentByBillerReferenceOrTransRef(
                            jsonObject.getString("BillerReference"), jsonObject.getString("TransRef"))
                    .ifPresentOrElse(payment -> {
                        if (!payment.getTransactionStatus().equals(0)) {
                            response.put("response", "999");
                            response.put("responseDescription", "The Transaction request has already been processed");
                        } else {
                            byte[] decodedBytes = Base64.getDecoder().decode(payment.getValidationCode());
                            String decodedString = new String(decodedBytes);
                            if (!decodedString.equals(jsonObject.getString("Code"))) {
                                response.put("response", "999");
                                response.put("responseDescription", "The Validation code is not valid");
                            } else {

                                JSONObject jsonRequest = new JSONObject();
                                jsonRequest.put("TransactionReqType", "BILL-PAYMENT");
                                jsonRequest.put("ClientReqType", payment.getClient());
                                jsonRequest.put("TransactionId", payment.getTransRef());
                                jsonRequest.put("CustomerAccount", payment.getPhoneNumber());
                                jsonRequest.put("Amount", payment.getAmount().toString());
                                jsonRequest.put("Narration", "Bill Payment Ref: " + payment.getBillerReference());
                                jsonRequest.put("Channel", "TDP");
                                RequestBuilder builder = new RequestBuilder("POST");
                                builder.addHeader("Content-Type", "application/json")
                                        .setBody(jsonRequest.toString())
                                        .setUrl(SP_URL)
                                        .build();

                                JSONObject jsonResponse = httpProcessor.processProperRequest(builder);

                                if (jsonResponse.getString("Status").equals("00")) {
                                    if (jsonResponse.getString("EnoughBalance").equals("0")) {
                                        payment.setResponseStaus("000");
                                        payment.setResponseDescription("Bill Payment was successful");
                                        payment.setTransactionStatus(3);
                                        //response.put("customerAccountBalance", jsonResponse.getString("CustomerAccountBalance"));
                                        response.put("transactionCost", jsonResponse.getString("TransactionCost"));
                                        response.put("billerCommission", jsonResponse.getString("BillerTransactionCommission"));
                                        response.put("response", "000");
                                        response.put("responseDescription", "Bill Payment was successful");
                                        response.put("amountPaid", payment.getAmount().toString());
                                        response.put("transRef", payment.getTransRef());
                                        payment.setCustomerCharge(new BigDecimal(jsonResponse.getString("TransactionCost")));
                                        payment.setTransactionCost(new BigDecimal(jsonResponse.getString("TransactionCost")));

                                        JSONObject jsonObj = new JSONObject();
                                        Date date = new Date();
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                                        String strDate = dateFormat.format(date);
                                        String strTime = timeFormat.format(date);
                                        jsonObj.put("TemplateId", TEMPLATE_ID_SUCCESS);
                                        jsonObj.put("Phone", payment.getPhoneNumber());
                                        jsonObj.put("name", globalMethods.getSahayName(payment.getPhoneNumber())); // PENDING VERIFICATION
                                        jsonObj.put("amount", payment.getAmount().toString());
                                        jsonObj.put("biller_name", jsonObject.getString("ClientName"));
                                        jsonObj.put("date", strDate);
                                        jsonObj.put("time", strTime);
                                        jsonObj.put("balance", jsonResponse.getString("CustomerAccountBalance"));
                                        jsonObj.put("Fee", jsonResponse.getString("TransactionCost"));
                                        jsonObj.put("Txn_Ref", payment.getTransRef());

                                        String[] words = {"name", "amount", "biller_name", "date", "time", "balance", "Fee", "Txn_Ref"};
                                        String message = smsLogging.generateMessage(jsonObj, words);
                                        globalMethods.sendSMS(message, payment.getPhoneNumber(), payment.getTransRef());
                                        if (payment.getClient().equals("LTMKN")) {
                                            if (jsonObject.getString("AccountType").equals("SAHAY")) {
                                                JSONObject transfer = new JSONObject();
                                                transfer.put("TransactionReqType", "BILL-ACCOUNT-FULFILMENT");
                                                transfer.put("ClientReqType", "BFFT");
                                                transfer.put("TransactionId", payment.getTransRef());
                                                transfer.put("FromAccount", jsonObject.getString("SettlementAccount"));
                                                transfer.put("ToAccount", jsonObject.getString("DriverAccountNumber"));
                                                transfer.put("Amount", payment.getAmount().toString());
                                                transfer.put("Channel", "API");
                                                transfer.put("Narration", "Bill Driver Fulfillment Ref: " + payment.getBillerReference());
                                                RequestBuilder build = new RequestBuilder("POST");
                                                build.addHeader("Content-Type", "application/json")
                                                        .setBody(transfer.toString())
                                                        .setUrl(SP_URL)
                                                        .build();
                                                JSONObject tranRes = httpProcessor.processProperRequest(builder);

                                                if (tranRes.getString("Status").equals("00")) {
                                                    response.put("fulfillResponse", "000");
                                                    response.put("fulfillResponseDescription", "Sahay Account was successfully credited");
                                                } else {
                                                    response.put("fulfillResponse", "999");
                                                    response.put("fulfillResponseDescription", "Sahay Account was not successfully credited");
                                                }
                                            } else {
                                                JSONObject transfer = new JSONObject();
                                                transfer.put("TransactionReqType", "BTAW");
                                                transfer.put("ClientReqType", payment.getClient());
                                                transfer.put("TransactionId", payment.getTransRef());
                                                transfer.put("BillAccount", jsonObject.getString("DriverAccountNumber"));
                                                transfer.put("ToAccount", jsonObject.getString("SettlementAccount"));
                                                transfer.put("PhoneNumber", jsonObject.getString("DriverPhoneNumber"));
                                                transfer.put("Amount", payment.getAmount().toString());
                                                transfer.put("Narration", "Driver Account Settlement Ref: " + payment.getBillerReference());
                                                transfer.put("Channel", "API");
                                                RequestBuilder build = new RequestBuilder("POST");
                                                build.addHeader("Content-Type", "application/json")
                                                        .setBody(jsonRequest.toString())
                                                        .setUrl(AWASH_URL)
                                                        .build();
                                                JSONObject tranRes = httpProcessor.processProperRequest(builder);

                                                if (tranRes.getString("response").equals("00")) {
                                                    response.put("fulfillResponse", "000");
                                                    response.put("fulfillResponseDescription", "Awash Account was successfully credited");
                                                } else {
                                                    response.put("fulfillResponse", "999");
                                                    response.put("fulfillResponseDescription", "Awash Account was not successfully credited");
                                                }
                                            }
                                        }
                                    } else {
                                        payment.setTransactionStatus(5);
                                        payment.setResponseDescription("Insufficient Balance");
                                        payment.setResponseStaus("999");
                                        response.put("response", "999");
                                        response.put("responseDescription", "Insufficient Balance");
                                    }
                                    response.put("customerName", jsonResponse.getString("CustomerName"));
                                } else {
                                    payment.setResponseStaus("999");
                                    payment.setResponseDescription("Bill Payment failed");
                                    payment.setTransactionStatus(1);
                                    response.put("response", "999");
                                    response.put("responseDescription", "Bill Payment failed");
                                }
                                payment.setResponsePayload(response.toString());
                                payment.setProcessedDatetime(Timestamp.from(Instant.now()));
                                paymentRepository.save(payment);
                            }
                        }
                    }, () -> {
                        response.put("response", "999");
                        response.put("responseDescription", "Transaction with the same reference exists");
                    });
        } catch (Exception ex) {
            log.log(Level.WARNING, "Stage Fulfilment Error : " + ex.getMessage());
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;
    }
}
