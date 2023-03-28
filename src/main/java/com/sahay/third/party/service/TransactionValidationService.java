package com.sahay.third.party.service;


import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.util.logging.Level;

@Log
@Component
@SuppressWarnings("Duplicates")
public class TransactionValidationService {
    @PersistenceContext
    private EntityManager entityManager;

    public JSONObject pickAndProcess(String account, String amount) {
        JSONObject transResponse = new JSONObject();
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("ValidateTransactionLimit");
            query.registerStoredProcedureParameter("InitiatingAccount", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("TranAmount", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("TranLimitExceeded", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("TranLimitAmount", String.class, ParameterMode.OUT);
            query.setParameter("InitiatingAccount", account);
            query.setParameter("TranAmount", amount);
            // execute SP
            query.execute();
            // get result
            transResponse.put("TranLimitExceeded", query.getOutputParameterValue("TranLimitExceeded"));
            transResponse.put("TranLimitAmount", query.getOutputParameterValue("TranLimitAmount"));
            transResponse.put("Status", "00");
            transResponse.put("Message", "The request was processed successfully");
        } catch (Exception ex) {
            log.log(Level.WARNING, "VALIDATION CLASS : " + ex.getMessage());
            transResponse.put("Status", "101");
            transResponse.put("TranLimitExceeded", "1");
            transResponse.put("Message", "Failed while processing the request");
        }
        return transResponse;
    }
}