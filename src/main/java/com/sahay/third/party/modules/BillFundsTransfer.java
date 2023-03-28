package com.sahay.third.party.modules;

import com.sahay.third.party.model.FundsTransferPayment;
import com.sahay.third.party.repo.FundsTransferPaymentRepository;
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
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class BillFundsTransfer {

    @Value(value = "${org.app.properties.paybill.payment.otp}")
    private String TEMPLATE_ID;

    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;

    @Value(value = "${org.app.properties.awash.endpoint}")
    private String AWASH_URL;

    private final SmsLogging smsLogging;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;
    private final AccountBalance accountBalance;
    private final ProcessingService processingService;
    private final FundsTransferPaymentRepository fundsTransferPaymentRepository;

    @Autowired
    public BillFundsTransfer(SmsLogging smsLogging,
                             GlobalMethods globalMethods,
                             HttpProcessor httpProcessor,
                             AccountBalance accountBalance,
                             ProcessingService processingService,
                             FundsTransferPaymentRepository fundsTransferPaymentRepository) {
        this.smsLogging = smsLogging;
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
        this.accountBalance = accountBalance;
        this.processingService = processingService;
        this.fundsTransferPaymentRepository = fundsTransferPaymentRepository;
    }

    public JSONObject pickAndProcess(JSONObject jsonObject) {
        JSONObject response = new JSONObject();
        try {
            fundsTransferPaymentRepository.findFundsTransferPaymentByBillerReference(jsonObject.getString("BillerReference"))
                    .ifPresentOrElse(payment -> {
                        response.put("response", "999");
                        response.put("responseDescription", "Transaction with the same reference exists");
                    }, () -> {
                        JSONObject accBalance = accountBalance.pickAndProcess(jsonObject);
                        if (Double.valueOf(accBalance.getDouble("FloatBalance")) > Double.valueOf(jsonObject.getString("Amount"))) {
                            String stagingRef = globalMethods.generateTrans();
                            FundsTransferPayment payment = new FundsTransferPayment();
                            payment.setClient(jsonObject.getString("Client"));
                            payment.setTransRef(stagingRef);
                            payment.setPhoneNumber(jsonObject.getString("PhoneNumber"));
                            payment.setBillerReference(jsonObject.getString("BillerReference"));
                            payment.setAmount(new BigDecimal(jsonObject.getString("Amount")));
                            payment.setAccountType(jsonObject.getString("AccountType"));
                            payment.setAccountNumber(jsonObject.getString("AccountNumber"));
                            payment.setTransactionStatus(0);
                            payment.setStatus(0);
                            payment.setCreatedDate(Timestamp.from(Instant.now()));
                            fundsTransferPaymentRepository.save(payment);

                            if (!jsonObject.getString("AccountType").equals("SAHAY")) {
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

                                if (acLookRes.getString("response").equals("999")) {
                                    payment.setTransactionStatus(5);
                                    payment.setThirdPartyPayload("");
                                    payment.setResponseDescription("The Account is not available.");
                                    payment.setResponseStaus("999");
                                    payment.setProcessedDatetime(Timestamp.from(Instant.now()));
                                    response.put("response", "999");
                                    response.put("responseDescription", "The Account is not available");
                                    fundsTransferPaymentRepository.save(payment);
                                } else {
                                    JSONObject transfer = new JSONObject();
                                    transfer.put("TransactionReqType", "BTAW");
                                    transfer.put("ClientReqType", payment.getClient());
                                    transfer.put("TransactionId", payment.getTransRef());
                                    transfer.put("BillAccount", jsonObject.getString("AccountNumber"));
                                    transfer.put("ToAccount", jsonObject.getString("SettlementAccount"));
                                    transfer.put("PhoneNumber", jsonObject.getString("PhoneNumber"));
                                    transfer.put("Amount", payment.getAmount().toString());
                                    transfer.put("Narration", "Driver Account Transfer Ref: " + payment.getBillerReference());
                                    transfer.put("Channel", "API");
                                    RequestBuilder awsTransferBuilder = new RequestBuilder("POST");
                                    payment.setThirdPartyPayload(transfer.toString());
                                    awsTransferBuilder.addHeader("Content-Type", "application/json")
                                            .setBody(transfer.toString())
                                            .setUrl(AWASH_URL)
                                            .build();
                                    JSONObject awsTransferRes = httpProcessor.processProperRequest(awsTransferBuilder);

                                    if (awsTransferRes.getString("response").equals("000")) {
                                        payment.setTransactionStatus(3);
                                        payment.setResponseDescription("success");
                                        payment.setResponseStaus("000");
                                        payment.setProcessedDatetime(Timestamp.from(Instant.now()));
                                        response.put("response", "000");
                                        response.put("responseDescription", "successful");
                                        fundsTransferPaymentRepository.save(payment);
                                    } else {
                                        payment.setTransactionStatus(5);
                                        payment.setResponseDescription(awsTransferRes.getString("responseDescription"));
                                        payment.setResponseStaus("999");
                                        payment.setProcessedDatetime(Timestamp.from(Instant.now()));
                                        response.put("response", "999");
                                        response.put("responseDescription", awsTransferRes.getString("responseDescription"));
                                        fundsTransferPaymentRepository.save(payment);
                                    }
                                }
                            } else {
                                JSONObject transPosting = new JSONObject();
                                transPosting.put("TransactionReqType", "BILL-FT");
                                transPosting.put("ClientReqType", "BT");
                                transPosting.put("TransactionId", payment.getTransRef());
                                transPosting.put("SettlementAccount", jsonObject.getString("SettlementAccount"));
                                transPosting.put("FromAccount", accBalance.getString("FloatAccount"));
                                transPosting.put("ToAccount", jsonObject.getString("AccountNumber"));
                                transPosting.put("Amount", payment.getAmount().toString());
                                transPosting.put("Narration", "Funds Settlement from Sahay Account : " + jsonObject.getString("AccountNumber"));
                                transPosting.put("Channel", "API");

                                payment.setTransactionPostPayload(transPosting.toString());
                                RequestBuilder transBuilder = new RequestBuilder("POST");
                                transBuilder.addHeader("Content-Type", "application/json")
                                        .setBody(transPosting.toString())
                                        .setUrl(SP_URL)
                                        .build();
                                JSONObject jsonTransResponse = httpProcessor.processProperRequest(transBuilder);

                                if (jsonTransResponse.getString("Status").equals("00")) {
                                    if (jsonTransResponse.getString("EnoughBalance").equals("0")) {
                                        payment.setTransactionStatus(3);
                                        payment.setResponseDescription("Funds Transfer was successful");
                                        payment.setResponseStaus("000");
                                        response.put("response", "000");
                                        response.put("responseDescription", "Funds Transfer was successful");
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
                                        fundsTransferPaymentRepository.save(payment);
                                    }
                                } else {
                                    payment.setResponseStaus("999");
                                    payment.setResponseDescription("Error in Transaction Payment failed");
                                    payment.setTransactionStatus(10);
                                    response.put("response", "999");
                                    response.put("responseDescription", "Error in Transaction Payment failed");
                                    fundsTransferPaymentRepository.save(payment);
                                }
                            }
                        } else {
                            response.put("response", "999");
                            response.put("responseDescription", "Insufficient Balance");
                        }
                    });
        } catch (Exception ex) {
            log.log(Level.WARNING, "Bill Funds Transfer Error : " + ex.getMessage());
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;
    }
}
