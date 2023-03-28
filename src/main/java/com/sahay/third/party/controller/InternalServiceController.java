package com.sahay.third.party.controller;

import com.sahay.third.party.modules.InternalBillPayment;
import com.sahay.third.party.modules.InternalGetService;
import com.sahay.third.party.repo.ClientRepository;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Level;

@RequestMapping("/third-party/api/v1/internal")
@Log
@RestController
@CrossOrigin(origins = "*")
@SuppressWarnings("Duplicates")
public class InternalServiceController {
    private final InternalGetService internalGetService;
    private final InternalBillPayment internalBillPayment;

    @Autowired
    public InternalServiceController(InternalGetService internalGetService,
                                     InternalBillPayment internalBillPayment) {
        this.internalGetService = internalGetService;
        this.internalBillPayment = internalBillPayment;
    }

    @RequestMapping(value = "/request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postRequest(@RequestBody String requestBody) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        JSONObject requestObject = new JSONObject(requestBody);
        String requestType = null;

        try {
            requestType = requestObject.getString("transactionType");
        } catch (Exception ex) {
            responseBody.put("response", "999");
            responseBody.put("responseDescription", "No request type provided");
            log.log(Level.SEVERE, "Request Type was not submitted");
            return responseBody.toString();
        }
        switch (requestType) {
            case "BILL-PAYMENT":
                responseBody = internalBillPayment.pickAndProcess(requestObject);
                break;
            case "GET-BUSINESS":
                responseBody = internalGetService.getClients();
                break;
            case "GET-PIN":
                responseBody = internalGetService.getCustomerPins(requestObject);
                break;
            default:
                responseBody.put("Status", "99");
                responseBody.put("Message", "Request Type Submitted is not within scope");
                log.log(Level.SEVERE, "Request Type Submitted is not within scope");
                break;
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }
}
