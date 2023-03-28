package com.sahay.third.party.controller;

import com.sahay.third.party.config.JwtTokenUtil;
import com.sahay.third.party.model.Client;
import com.sahay.third.party.modules.*;
import com.sahay.third.party.modules.transfer.EthioSwitchService;
import com.sahay.third.party.object.ClientLogin;
import com.sahay.third.party.object.CreateClient;
import com.sahay.third.party.repo.ClientRepository;
import com.sahay.third.party.repo.PaymentRepository;
import com.sahay.third.party.service.GlobalMethods;
import com.sahay.third.party.service.JwtUserDetailsService;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;


@Log
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/third-party/api/v1/")
@SuppressWarnings("Duplicates")
public class ServiceController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private JwtUserDetailsService userDetailsService;
    @Autowired
    private PasswordEncoder bcryptEncoder;

    private final GlobalMethods globalMethods;
    private final AccountBalance accountBalance;
    private final ClientRepository clientRepository;
    private final StageBillPayment stageBillPayment;
    private final BillFundsTransfer billFundsTransfer;
    private final FullFilBillPayment fullFilBillPayment;
    private final PaymentRepository paymentRepository;
    private final EthioSwitchService ethioSwitchService;
    private final CustomerValidation customerValidation;
    private final BillAirtimePayment billAirtimePayment;
    private final FundsTransferService fundsTransferService;

    @Autowired
    public ServiceController(GlobalMethods globalMethods,
                             AccountBalance accountBalance,
                             ClientRepository clientRepository,
                             StageBillPayment stageBillPayment,
                             BillFundsTransfer billFundsTransfer,
                             FullFilBillPayment fullFilBillPayment,
                             PaymentRepository paymentRepository,
                             EthioSwitchService ethioSwitchService,
                             CustomerValidation customerValidation,
                             BillAirtimePayment billAirtimePayment,
                             FundsTransferService fundsTransferService) {
        this.globalMethods = globalMethods;
        this.accountBalance = accountBalance;
        this.clientRepository = clientRepository;
        this.stageBillPayment = stageBillPayment;
        this.billFundsTransfer = billFundsTransfer;
        this.fullFilBillPayment = fullFilBillPayment;
        this.paymentRepository = paymentRepository;
        this.ethioSwitchService = ethioSwitchService;
        this.customerValidation = customerValidation;
        this.billAirtimePayment = billAirtimePayment;
        this.fundsTransferService = fundsTransferService;
    }

    @RequestMapping(value = "/create-login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postRequest(@RequestBody ClientLogin requestBody) {
        JSONObject responseBody = new JSONObject();

        clientRepository.findClientByClientReqType(requestBody.getConsumerKey())
                .ifPresentOrElse(client -> {
                    client.setPassword(bcryptEncoder.encode(requestBody.getConsumerSecret()));
                    clientRepository.save(client);
                    responseBody.put("Status", "00");
                    responseBody.put("Message", "Successful");
                }, () -> {
                    responseBody.put("Status", "99");
                    responseBody.put("Message", "Invalid Username");
                });
        return responseBody.toString();
    }

    @RequestMapping(value = "/add-client", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postAddClient(@RequestBody CreateClient requestBody) {
        JSONObject responseBody = new JSONObject();

        clientRepository.findClientByClientReqType(requestBody.getClientReqType())
                .ifPresentOrElse(client -> {
                    responseBody.put("Status", "99");
                    responseBody.put("Message", "Client Definition already exists : " + client.getClientName());
                }, () -> {
                    Client client = new Client();
                    String userName = globalMethods.generateRandomUsername(10);
                    String password = globalMethods.generateRandomUsername(20);
                    client.setClientName(requestBody.getClientName());
                    client.setClientReqType(requestBody.getClientReqType());
                    client.setClientAccount(requestBody.getClientAccount());
                    client.setNotUrl(requestBody.getNotUrl());
                    client.setType(requestBody.getType());
                    client.setUsername(userName);
                    client.setPassword(bcryptEncoder.encode(password));
                    client.setCreatedDate(Timestamp.from(Instant.now()));
                    clientRepository.save(client);
                    responseBody.put("ConsumerKey", userName);
                    responseBody.put("ConsumerSecret", password);
                    responseBody.put("Status", "00");
                    responseBody.put("Message", "Successfully saved the client");
                });
        return responseBody.toString();
    }

    @RequestMapping(value = "/generate-token", method = RequestMethod.POST)
    public ResponseEntity<?> userAuthentication(@RequestBody ClientLogin authRequest) {
        JSONObject responseMap = new JSONObject();
        try {
            authenticate(authRequest.getConsumerKey(), authRequest.getConsumerSecret());
        } catch (Exception e) {
            responseMap.put("Status", "99");
            responseMap.put("Message", "Invalid Credentials");
            return ResponseEntity.ok(responseMap.toString());
        }
        try {
            final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getConsumerKey());
            String token = jwtTokenUtil.generateToken(userDetails);
            responseMap.put("Status", "00");
            responseMap.put("AccessToken", token);
            responseMap.put("Message", "Login Successfully");
            return ResponseEntity.ok(responseMap.toString());
        } catch (Exception e) {
            log.log(Level.INFO, e.getMessage());
            responseMap.put("Status", "99");
            responseMap.put("Message", "Invalid Credentials");
            return ResponseEntity.ok(responseMap.toString());
        }
    }

    private void authenticate(String username, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }

    @RequestMapping(value = "/check-customer", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String checkCustomerExists(@RequestBody String requestBody,
                                      HttpServletRequest request) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            requestObject.put("Client", globalMethods.getClientReqType());
            responseBody = customerValidation.pickAndProcess(requestObject);
        } catch (Exception ex) {
            responseBody.put("response", "101");
            responseBody.put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }

    @RequestMapping(value = "/check-ethio-banks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getAvailableBanks() {
        JSONObject responseBody = new JSONObject();
        try {
            responseBody = ethioSwitchService.availableBanks();
        } catch (Exception ex) {
            responseBody.put("response", "101");
            responseBody.put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }

    @RequestMapping(value = "/check-ethio-account", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String checkEthioAccount(@RequestBody String requestBody,
                                    HttpServletRequest request) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            requestObject.put("Client", globalMethods.getClientReqType());
            requestObject.put("SettlementAccount", globalMethods.getClientSettlementAccount());
            responseBody = ethioSwitchService.accountCheck(requestObject);
        } catch (Exception ex) {
            responseBody.put("response", "101");
            responseBody.put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }

    @RequestMapping(value = "/stage-request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postStageRequest(@RequestBody String requestBody,
                                   HttpServletRequest request) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            requestObject.put("Client", globalMethods.getClientReqType());
            requestObject.put("ClientName", globalMethods.getClientName());
            responseBody = stageBillPayment.pickAndProcess(requestObject);
        } catch (Exception ex) {
            responseBody.put("response", "101");
            responseBody.put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }

    @RequestMapping(value = "/fulfill-request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postFulFillRequest(@RequestBody String requestBody,
                                     HttpServletRequest request) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            requestObject.put("ClientName", globalMethods.getClientName());
            requestObject.put("SettlementAccount", globalMethods.getClientSettlementAccount());
            responseBody = fullFilBillPayment.pickAndProcess(requestObject);
        } catch (Exception ex) {
            responseBody.put("response", "101");
            responseBody.put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }

    @RequestMapping(value = "/airtime-purchase", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postAirtimePurchase(@RequestBody String requestBody,
                                      HttpServletRequest request) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            requestObject.put("Client", globalMethods.getClientReqType());
            requestObject.put("ClientName", globalMethods.getClientName());
            requestObject.put("SettlementAccount", globalMethods.getClientSettlementAccount());
            responseBody = billAirtimePayment.pickAndProcess(requestObject);
        } catch (Exception ex) {
            responseBody.put("response", "101");
            responseBody.put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }

    @RequestMapping(value = "/bill-funds-transfer", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String postFundsTransfer(@RequestBody String requestBody,
                                    HttpServletRequest request) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            requestObject.put("Client", globalMethods.getClientReqType());
            requestObject.put("ClientName", globalMethods.getClientName());
            requestObject.put("SettlementAccount", globalMethods.getClientSettlementAccount());
            responseBody = billFundsTransfer.pickAndProcess(requestObject);
        } catch (Exception ex) {
            responseBody.put("response", "101");
            responseBody.put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }

    @RequestMapping(value = "/funds-transfer", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String fundsTransfer(@RequestBody String requestBody,
                                HttpServletRequest request) {
        log.log(Level.INFO, requestBody);
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject requestObject = new JSONObject(requestBody);
            requestObject.put("Client", globalMethods.getClientReqType());
            requestObject.put("ClientName", globalMethods.getClientName());
            requestObject.put("SettlementAccount", globalMethods.getClientSettlementAccount());
            responseBody = fundsTransferService.pickAndProcess(requestObject);
        } catch (Exception ex) {
            responseBody.put("response", "101");
            responseBody.put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }



    @RequestMapping(value = "/trans-status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getTransactionStatus(@RequestBody String requestBody,
                                       HttpServletRequest request) {
        log.log(Level.INFO, requestBody);
        AtomicReference<JSONObject> responseBody = new AtomicReference<>(new JSONObject());
        try {
            JSONObject jsonObject = new JSONObject(requestBody);
            paymentRepository.findPaymentByBillerReferenceOrTransRef(
                            jsonObject.getString("BillerReference"), jsonObject.getString("TransRef"))
                    .ifPresentOrElse(payment -> {
                        responseBody.set(new JSONObject(payment.getResponsePayload()));
                    }, () -> {
                        responseBody.get().put("response", "999");
                        responseBody.get().put("responseDescription", "The Biller or Transaction Reference is not found ");
                    });
        } catch (Exception ex) {
            responseBody.get().put("response", "101");
            responseBody.get().put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }


    @RequestMapping(value = "/bill-trans-status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getInternalTransactionStatus(@RequestBody String requestBody,
                                               HttpServletRequest request) {
        log.log(Level.INFO, requestBody);
        AtomicReference<JSONObject> responseBody = new AtomicReference<>(new JSONObject());
        try {
            JSONObject jsonObject = new JSONObject(requestBody);
            paymentRepository.findPaymentByTransRefOrAccountNumberOrPhoneNumber(
                            jsonObject.getString("TransRef"),
                            jsonObject.getString("AccountNumber"),
                            jsonObject.getString("CustomerAccount"))
                    .ifPresentOrElse(payment -> {
                        responseBody.set(new JSONObject(payment.getResponsePayload()));
                    }, () -> {
                        responseBody.get().put("response", "999");
                        responseBody.get().put("responseDescription", "The Biller or Transaction Reference is not found ");
                    });
        } catch (Exception ex) {
            responseBody.get().put("response", "101");
            responseBody.get().put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }

    @RequestMapping(value = {"/account-balance"}, method = {RequestMethod.GET}, produces = {"application/json"})
    @ResponseBody
    public String getAccountBalance() {
        JSONObject responseBody = new JSONObject();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("SettlementAccount", globalMethods.getClientSettlementAccount());
            responseBody = accountBalance.pickAndProcess(jsonObject);
        } catch (Exception ex) {
            responseBody.put("response", "101");
            responseBody.put("responseDescription", "Request Type was not submitted");
        }
        log.log(Level.INFO, responseBody.toString());
        return responseBody.toString();
    }
}
