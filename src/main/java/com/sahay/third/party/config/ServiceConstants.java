package com.sahay.third.party.config;

import org.springframework.stereotype.Component;

@Component
public class ServiceConstants {

    /**
     * Booking Statuses
     */
    public static final Integer bookPending = 0;
    public static final Integer bookedPaymentInProgress = 1;
    public static final Integer bookDeclinedPayment = 2;
    public static final Integer bookSuccessInProgress = 3;
    public static final Integer bookReturned = 4;
    public static final Integer bookCancelled = 5;
    public static final Integer bookRefunded = 10;

    /**
     * Customer Profile Statuses
     */
    public static final Integer profilePending = 0;
    public static final Integer profileValidated = 1;
    public static final Integer profileConfirmed = 3;
    public static final Integer profileDenied = 5;


    /*
     * Payment Types
     *
     */
    public static String rentalPayment = "RENTAL";
    public static String penaltyPayment = "PENALTY";
    public static String extensionPayment = "EXTENSION";
    public static String bookingPayment = "BOOKING";
    public static String savingsDeposit = "SAVINGS";


    /*
     *
     * Transaction Status
     */
    public static final Integer TRANSACTION_INITIAL = 0;
    public static final Integer TRANSACTION_STARTED_PROCESSING = 1;
    public static final Integer TRANSACTION_FAILED = 5;
    public static final Integer TRANSACTION_VALIDATING = 4;
    public static final Integer TRANSACTION_SUCCESS = 3;
}
