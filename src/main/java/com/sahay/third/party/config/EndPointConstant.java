package com.sahay.third.party.config;

public class EndPointConstant {
    public static String versioning = "/third-party/api/v1";

    public static String[] whitelistEndpoints = {
            versioning + "/generate-token",
            versioning + "/create-login",
            versioning + "/add-client",
            versioning + "/internal/request"
    };
}
