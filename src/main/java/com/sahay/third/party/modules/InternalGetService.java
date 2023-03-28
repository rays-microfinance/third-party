package com.sahay.third.party.modules;

import com.sahay.third.party.repo.ClientRepository;
import com.sahay.third.party.repo.PaymentRepository;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log
@Component
@SuppressWarnings("Duplicates")
public class InternalGetService {
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;

    public InternalGetService(ClientRepository clientRepository, PaymentRepository paymentRepository) {
        this.clientRepository = clientRepository;
        this.paymentRepository = paymentRepository;
    }

    public JSONObject getClients() {
        JSONObject response = new JSONObject();
        try {
            List<JSONObject> businesses = new ArrayList<>();
            clientRepository.findAll().forEach(client -> {
                JSONObject business = new JSONObject();
                business.put("ClientId", client.getId());
                business.put("ClientName", client.getClientName());
                business.put("ClientReqType", client.getClientReqType());
                businesses.add(business);
            });
            response.put("response", "000");
            response.put("responseDescription", "successful");
            response.put("businesses", businesses);
        } catch (Exception ex) {
            response.put("response", "999");
            response.put("responseDescription", "Failure. Error on processing");
        }
        return response;
    }

    public JSONObject getCustomerPins(JSONObject reqBody) {
        JSONObject response = new JSONObject();
        try {
            List<JSONObject> custPins = new ArrayList<>();
            paymentRepository.findPaymentsByPhoneNumber(reqBody.getString("msisdn")).forEach(payment -> {
                Date dNow = new Date();
                Timestamp timestamp = new Timestamp(dNow.getTime());
                if (!timestamp.after(payment.getValidationExpiryDate())) {
                    JSONObject custPin = new JSONObject();
                    custPin.put("clientName", clientRepository.findClientByClientReqType(payment.getClient()).get().getClientName());
                    byte[] decodedBytes = Base64.getDecoder().decode(payment.getValidationCode());
                    String decodedString = new String(decodedBytes);
                    custPin.put("otp", decodedString);
                    long diff = payment.getValidationExpiryDate().getTime() - timestamp.getTime();//as given
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                    custPin.put("IsExpired", "False");
                    custPin.put("expiresIn", minutes);
                    custPins.add(custPin);
                }
            });
            response.put("response", "000");
            response.put("responseDescription", "successful");
            response.put("custPins", custPins);
        } catch (Exception ex) {
            response.put("response", "999");
            response.put("responseDescription", "Failure. Error on processing");
        }
        return response;
    }
}
