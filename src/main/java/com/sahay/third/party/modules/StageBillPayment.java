package com.sahay.third.party.modules;

import com.sahay.third.party.model.Payment;
import com.sahay.third.party.repo.PaymentRepository;
import com.sahay.third.party.service.GlobalMethods;
import com.sahay.third.party.service.SmsLogging;
import com.sahay.third.party.service.TransactionValidationService;
import lombok.extern.java.Log;
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
public class StageBillPayment {

    @Value(value = "${org.app.properties.paybill.payment.otp}")
    private String TEMPLATE_ID;

    @Value(value = "${oorg.app.properties.paybill.payment.limit}")
    private String LIMIT_TEMPLATE_ID;

    private final SmsLogging smsLogging;
    private final GlobalMethods globalMethods;
    private final PaymentRepository paymentRepository;
    private final ProcessingService processingService;
    private final TransactionValidationService transactionValidationService;

    @Autowired
    public StageBillPayment(SmsLogging smsLogging,
                            GlobalMethods globalMethods,
                            PaymentRepository paymentRepository,
                            ProcessingService processingService,
                            TransactionValidationService transactionValidationService) {
        this.smsLogging = smsLogging;
        this.globalMethods = globalMethods;
        this.paymentRepository = paymentRepository;
        this.processingService = processingService;
        this.transactionValidationService = transactionValidationService;
    }

    public JSONObject pickAndProcess(JSONObject jsonObject) {
        JSONObject response = new JSONObject();
        try {

            paymentRepository.findPaymentByBillerReference(jsonObject.getString("BillerReference"))
                    .ifPresentOrElse(payment -> {
                        response.put("response", "999");
                        response.put("responseDescription", "Transaction with the same reference exists");
                    }, () -> {
                        JSONObject cusRes = globalMethods.checkCustomerExists(jsonObject);

                        if (cusRes.getString("Status").equals("SUCCESS")) {
                            JSONObject validBody = transactionValidationService.pickAndProcess(
                                    jsonObject.getString("PhoneNumber"), jsonObject.getString("Amount"));
                            log.log(Level.INFO, "TransRes : " + validBody.toString());

                            String stagingRef = globalMethods.generateTrans();
                            if (validBody.getString("TranLimitExceeded").equals("1")) {
                                response.put("limitAmount", validBody.getString("TranLimitAmount"));
                                response.put("response", "999");
                                response.put("responseDescription", "Customer Account cannot transfer more than ETB " + validBody.getString("TranLimitAmount"));

                                JSONObject jsonObj = new JSONObject();

                                jsonObj.put("TemplateId", LIMIT_TEMPLATE_ID);
                                jsonObj.put("Phone", jsonObject.getString("PhoneNumber"));
                                jsonObj.put("name", cusRes.getString("Names"));
                                jsonObj.put("company_name", jsonObject.getString("ClientName"));
                                jsonObj.put("limit", validBody.getString("TranLimitAmount"));
                                String[] words = {"name", "limit", "company_name"};
                                String sms = smsLogging.generateMessage(jsonObj, words);
                                globalMethods.sendSMS(sms, jsonObject.getString("PhoneNumber"), stagingRef);
                            } else {
                                String uniqueCode = globalMethods.getRemicode();
                                Payment payment = new Payment();
                                payment.setClient(jsonObject.getString("Client"));
                                payment.setTransRef(stagingRef);
                                payment.setPhoneNumber(jsonObject.getString("PhoneNumber"));
                                payment.setBillerReference(jsonObject.getString("BillerReference"));
                                payment.setAccountNumber(jsonObject.getString("PhoneNumber"));
                                payment.setAmount(new BigDecimal(jsonObject.getString("Amount")));
                                payment.setValidationCode(Base64.getEncoder().encodeToString(uniqueCode.getBytes()));
                                Date dNow = new Date();
                                Timestamp timestamp = new Timestamp(dNow.getTime());
                                Timestamp validationDate = new Timestamp(timestamp.getTime() + (1000 * 60 * 5));
                                payment.setValidationExpiryDate(validationDate);
                                payment.setTransactionStatus(0);
                                payment.setRequestPayload(jsonObject.toString());
                                payment.setRequestDate(Timestamp.from(Instant.now()));
                                paymentRepository.save(payment);
                                response.put("response", "000");
                                response.put("responseDescription", "The Transaction Request has been successfully staged");
                                response.put("sahayRef", stagingRef);

                                JSONObject jsonObj = new JSONObject();
                                Date date = new Date();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                                String strDate = dateFormat.format(date);
                                String strTime = timeFormat.format(date);
                                jsonObj.put("TemplateId", TEMPLATE_ID);
                                jsonObj.put("Phone", payment.getPhoneNumber());
                                jsonObj.put("name", cusRes.getString("Names"));
                                jsonObj.put("amount", jsonObject.getString("Amount"));
                                jsonObj.put("company_name", jsonObject.getString("ClientName"));
                                jsonObj.put("date", strDate);
                                jsonObj.put("time", strTime);
                                jsonObj.put("otp", uniqueCode);

                                String[] words = {"name", "amount", "company_name", "date", "time", "otp"};
                                String message = smsLogging.generateMessage(jsonObj, words);
                                globalMethods.sendSMS(message, payment.getPhoneNumber(), payment.getTransRef());
                            }
                        } else {
                            response.put("response", "999");
                            response.put("responseDescription", "The account does not exist");
                        }
                    });
        } catch (Exception ex) {
            log.log(Level.WARNING, "Stage Billing Error : " + ex.getMessage());
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;
    }
}
