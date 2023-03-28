package com.sahay.third.party.modules;

import com.sahay.third.party.model.FundsTransferPayment;
import com.sahay.third.party.modules.transfer.AwashBankService;
import com.sahay.third.party.modules.transfer.EthioSwitchService;
import com.sahay.third.party.modules.transfer.SahayService;
import com.sahay.third.party.repo.FundsTransferPaymentRepository;
import com.sahay.third.party.service.GlobalMethods;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class FundsTransferService {

    private final SahayService sahayService;
    private final GlobalMethods globalMethods;
    private final AccountBalance accountBalance;
    private final AwashBankService awashBankService;
    private final EthioSwitchService ethioSwitchService;
    private final FundsTransferPaymentRepository fundsTransferPaymentRepository;

    @Autowired
    public FundsTransferService(SahayService sahayService,
                                GlobalMethods globalMethods,
                                AccountBalance accountBalance,
                                AwashBankService awashBankService,
                                EthioSwitchService ethioSwitchService,
                                FundsTransferPaymentRepository fundsTransferPaymentRepository) {

        this.sahayService = sahayService;
        this.globalMethods = globalMethods;
        this.accountBalance = accountBalance;
        this.awashBankService = awashBankService;
        this.ethioSwitchService = ethioSwitchService;
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

                            response.put("sahayRef", stagingRef);
                            jsonObject.put("TransRef", stagingRef);
                            jsonObject.put("FloatAccount", accBalance.getString("FloatAccount"));
                            jsonObject.put("Amount", payment.getAmount().toString());
                            jsonObject.put("PhoneNumber", payment.getPhoneNumber());
                            jsonObject.put("AccountNumber", jsonObject.getString("AccountNumber"));

                            JSONObject payResponse = new JSONObject();
                            switch (jsonObject.getString("AccountType")) {
                                case "SAHAY":
                                    payResponse = sahayService.pickAndProcess(jsonObject);
                                    break;
                                case "ETHIO-SWITCH":
                                    payResponse = ethioSwitchService.fundsTransfer(jsonObject);
                                    break;
                                case "AWASH":
                                    payResponse = awashBankService.pickAndProcess(jsonObject);
                                    break;
                                default:
                                    payResponse.put("ThirdPartyPayload", "");
                                    payResponse.put("TransactionPostPayload", "");
                                    payResponse.put("response", "999");
                                    payResponse.put("responseDescription", "Error in Transaction Payment failed");
                                    break;
                            }
                            payment.setTransactionPostPayload(payResponse.getString("TransactionPostPayload"));
                            payment.setThirdPartyPayload(payResponse.getString("ThirdPartyPayload"));
                            payment.setResponsePayload(payResponse.getString("ThirdPartyResponse"));
                            payment.setProcessedDatetime(Timestamp.from(Instant.now()));

                            if (payResponse.getString("response").equals("000")) {
                                payment.setTransactionStatus(3);
                                payment.setResponseDescription("Funds Transfer was successful");
                                payment.setResponseStaus("000");
                                response.put("response", "000");
                                response.put("responseDescription", "Funds Transfer was successful");
                            } else {
                                payment.setTransactionStatus(5);
                                payment.setResponseDescription(payResponse.getString("responseDescription"));
                                payment.setResponseStaus("999");
                                response.put("response", "999");
                                response.put("responseDescription", payResponse.getString("responseDescription"));
                            }
                            fundsTransferPaymentRepository.save(payment);
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
