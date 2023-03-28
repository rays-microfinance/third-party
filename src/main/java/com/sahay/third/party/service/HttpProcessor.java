package com.sahay.third.party.service;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.java.Log;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.asynchttpclient.Dsl.asyncHttpClient;

@Component
@Log
public class HttpProcessor {
    private AsyncHttpClientConfig cf;

    public HttpProcessor() {
        io.netty.handler.ssl.SslContext sc = null;
        try {
            sc = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } catch (SSLException e) {
            e.printStackTrace();
        }
        cf = new DefaultAsyncHttpClientConfig.Builder()
                .setCompressionEnforced(true)
                .setMaxConnections(100)
                .setPooledConnectionIdleTimeout(20000)
                .setRequestTimeout(20000)
                .setMaxConnectionsPerHost(5000)
                .setSslContext(sc)
                .build();
    }

    public JSONObject processProperRequest(RequestBuilder builder) {
        AtomicReference<JSONObject> responseBody = new AtomicReference<>(new JSONObject());
        try (AsyncHttpClient client = asyncHttpClient(cf)) {
            client
                    .executeRequest(builder)
                    .toCompletableFuture()
                    .thenApply(response -> {
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, response.getResponseBody());
                        JSONObject jsObject = new JSONObject();
                        try {
                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, String.format("Received [HttpStatus = %d, Body = %s, URL = %s] processing halted!", response.getStatusCode(), response.getResponseBody(), jsObject.toString()));
                            responseBody.set(new JSONObject(response.getResponseBody()));
                        } catch (Exception e) {
                            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, String.format("JSON Processing failed, [Message= %s, URL= %s]", e.getMessage(), ""));
                        }
                        return jsObject;
                    }).thenAccept(u -> Logger.getLogger(this.getClass().getName()).log(Level.FINE, u.toString())).join();
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, String.format("Micro-Service Sending Failed,[Type= %s,Message= %s]", "", e.getMessage()));
        }
        return responseBody.get();
    }

    public JSONObject jsonRequestProcessor(RequestBuilder builder) {
        AtomicReference<JSONObject> responseBody = new AtomicReference<>(new JSONObject());
        try (AsyncHttpClient client = asyncHttpClient(cf)) {
            client
                    .executeRequest(builder)
                    .toCompletableFuture()
                    .thenApply(response -> {
                        Logger.getLogger(this.getClass().getName()).log(Level.FINE, response.getResponseBody());
                        JSONObject jsObject = new JSONObject();
                        try {
                            responseBody.get().put("StatusCode", String.valueOf(response.getStatusCode()));
                            responseBody.get().put("StatusText", response.getStatusText());
                            responseBody.get().put("ResponseBody", response.getResponseBody());
                            Logger.getLogger(this.getClass().getName()).log(Level.INFO, String.format("Received [HttpStatus = %d, Text = %s, Body = %s] processing halted!", response.getStatusCode(), response.getStatusText(), response.getResponseBody()));
                        } catch (Exception e) {
                            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, String.format("JSON Processing failed, [Message= %s, URL= %s]", e.getMessage(), ""));
                        }
                        return jsObject;
                    }).thenAccept(u -> Logger.getLogger(this.getClass().getName()).log(Level.FINE, u.toString())).join();
        } catch (Exception e) {
            responseBody.get().put("StatusCode", "999");
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, String.format("Micro-Service Sending Failed,[Type= %s,Message= %s]", "", e.getMessage()));
        }
        return responseBody.get();
    }
}
