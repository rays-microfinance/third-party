package com.sahay.third.party.service;


import com.sahay.third.party.object.FloatTransferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {


    @Value("${}")
    private static final String ESB_URL = "";
    private final String COMPANY_CODE = "SHOA";
    private final String CHANNEL = "API";

    private final HttpProcessor httpRequest;
    // TODO: 7/20/2023 : FLOAT TRANSFER

    //BFTSF - Settlement to Float
    //BFTCF - Commission to Float
    //BFTCS - Commission to Settlement

    public JSONObject floatTransfer(FloatTransferRequest transferRequest) {

//        {
//            "username": "channel",
//                "password": "$_@C0NNEKT",
//                "messageType": "messageType",
//                "serviceCode": "serviceCode",
//                "transactionId": "120056016815",
//                "transactionType": "BFTSF,BFTCF,BFTCS",
//                "companyCode": "SHWAT",
//                "fromAccount": "9001001005",
//                "toAccount": "9001001006",
//                "amount": "1000",
//                "timestamp": "20200101120000",
//                "channel": "API"
//        }
        var request = new JSONObject();
        request.put("username", "channel");
        request.put("password", "$_@C0NNEKT");
        request.put("messageType", "1200");
        request.put("serviceCode", "286");
        ;
        request.put("transactionId", Timestamp.from(Instant.now()));
        request.put("transactionType", transferRequest.getTransactionType());
        request.put("companyCode", COMPANY_CODE);
        request.put("fromAccount", transferRequest.getFromAccount());
        request.put("toAccount", transferRequest.getAmount());
        request.put("timestamp", Timestamp.valueOf(LocalDateTime.now()));
        request.put("channel", CHANNEL);


        log.info("FLOAT TRANSFER : {}" , request);

        var floatTransferRequest = new RequestBuilder("POST");

        floatTransferRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setUrl(ESB_URL)
                .setBody(request.toString())
                .build();


        var floatTransferResponse = httpRequest.processProperRequest(floatTransferRequest);

        log.info("FLOAT TRANSFER RESPONSE : {}" , floatTransferResponse );


        return  floatTransferResponse;
    }

}
