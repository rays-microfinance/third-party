package com.sahay.third.party.modules;

import com.sahay.third.party.model.Payment;
import com.sahay.third.party.repo.ClientRepository;
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
public class InternalBillPayment {
    @Value(value = "${org.app.properties.sp.endpoint}")
    private String SP_URL;

    @Value(value = "${org.app.properties.paybill.payment.success}")
    private String TEMPLATE_ID_SUCCESS;

    private final SmsLogging smsLogging;
    private final GlobalMethods globalMethods;
    private final HttpProcessor httpProcessor;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public InternalBillPayment(SmsLogging smsLogging,
                               GlobalMethods globalMethods,
                               HttpProcessor httpProcessor,
                               ClientRepository clientRepository,
                               PaymentRepository paymentRepository) {
        this.smsLogging = smsLogging;
        this.globalMethods = globalMethods;
        this.httpProcessor = httpProcessor;
        this.clientRepository = clientRepository;
        this.paymentRepository = paymentRepository;
    }

    public JSONObject pickAndProcess(JSONObject jsonObject) {
        JSONObject response = new JSONObject();
        try {
            clientRepository.findClientByClientReqType(jsonObject.getString("Client"))
                    .ifPresentOrElse(client -> {
                        String stagingRef = globalMethods.generateTrans();
                        String uniqueCode = globalMethods.getRemicode();
                        Payment payment = new Payment();
                        payment.setClient(jsonObject.getString("Client"));
                        payment.setTransRef(stagingRef);
                        payment.setPhoneNumber(jsonObject.getString("PhoneNumber"));
                        payment.setBillerReference(stagingRef);
                        payment.setAccountNumber(jsonObject.getString("AccountNumber"));
                        payment.setAmount(new BigDecimal(jsonObject.getString("Amount")));
                        payment.setValidationCode(Base64.getEncoder().encodeToString(uniqueCode.getBytes()));
                        Date dNow = new Date();
                        Timestamp timestamp = new Timestamp(dNow.getTime());
                        Timestamp validationDate = new Timestamp(timestamp.getTime() + (1000 * 60 * 5));
                        payment.setValidationExpiryDate(validationDate);
                        payment.setTransactionStatus(0);
                        payment.setRequestPayload(jsonObject.toString());
                        payment.setRequestDate(Timestamp.from(Instant.now()));

                        JSONObject jsonRequest = new JSONObject();
                        jsonRequest.put("TransactionReqType", "BILL-PAYMENT");
                        jsonRequest.put("ClientReqType", payment.getClient());
                        jsonRequest.put("TransactionId", payment.getTransRef());
                        jsonRequest.put("CustomerAccount", payment.getPhoneNumber());
                        jsonRequest.put("Amount", payment.getAmount().toString());
                        jsonRequest.put("Narration", "Bill Payment Account : " + payment.getAccountNumber());
                        jsonRequest.put("Channel", "USSD");
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
                                response.put("customerAccountBalance", jsonResponse.getString("CustomerAccountBalance"));
                                response.put("transactionCost", jsonResponse.getString("TransactionCost"));
                                response.put("billerCommission", jsonResponse.getString("BillerTransactionCommission"));
                                response.put("response", "000");
                                response.put("responseDescription", "Bill Payment was successful");
                                response.put("amountPaid", payment.getAmount().toString());
                                response.put("transRef", payment.getTransRef());

                                payment.setCustomerCharge(new BigDecimal(jsonResponse.getString("TransactionCost")));
                                payment.setTransactionCost(new BigDecimal(jsonResponse.getString("TransactionCost")));
                                payment.setProcessedDatetime(Timestamp.from(Instant.now()));
                                paymentRepository.save(payment);

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
                                jsonObj.put("biller_name", client.getClientName());
                                jsonObj.put("date", strDate);
                                jsonObj.put("time", strTime);
                                jsonObj.put("balance", jsonResponse.getString("CustomerAccountBalance"));
                                jsonObj.put("Fee", jsonResponse.getString("TransactionCost"));
                                jsonObj.put("Txn_Ref", payment.getTransRef());

                                String[] words = {"name", "amount", "biller_name", "date", "time", "balance", "Fee", "Txn_Ref"};
                                String message = smsLogging.generateMessage(jsonObj, words);
                                globalMethods.sendSMS(message, payment.getPhoneNumber(), payment.getTransRef());

                                // Send Notifications to Client
                                JSONObject notRequest = new JSONObject();
                                notRequest.put("institution", client.getClientName());
                                notRequest.put("accountNumber", jsonObject.getString("AccountNumber"));
                                notRequest.put("amount", payment.getAmount().toString());
                                notRequest.put("tranDate", payment.getProcessedDatetime().toString());
                                notRequest.put("phoneNumber", payment.getPhoneNumber());
                                notRequest.put("txnRef", payment.getTransRef());
                                notRequest.put("payerName", globalMethods.getSahayName(payment.getPhoneNumber()));

                                RequestBuilder notBuilder = new RequestBuilder("POST");
                                notBuilder.addHeader("Content-Type", "application/json")
                                        .setBody(notRequest.toString())
                                        .setUrl(client.getNotUrl().trim())
                                        .build();
                                JSONObject notResponse = httpProcessor.processProperRequest(notBuilder);
                                log.log(Level.INFO, "Client [ " + client.getClientReqType() + " ] Notification Response : " + notResponse.toString());
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
                    }, () -> {
                        response.put("response", "999");
                        response.put("responseDescription", "The Bill Payment Client does not exist");
                    });

        } catch (Exception ex) {
            log.log(Level.WARNING, "Bill Payment Error : " + ex.getMessage());
            response.put("response", "999");
            response.put("responseDescription", "Error in Transaction Processing");
        }
        return response;
    }
}
