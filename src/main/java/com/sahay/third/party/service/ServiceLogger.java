package com.sahay.third.party.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceLogger {
    private static String LOGS_PATH = "";

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void createLog(String details, String uniqueId, int logLevel) {
        LOGS_PATH = System.getProperty("user.dir") + File.separator + "Logs";
        String typeOfLog = "";
        switch (logLevel) {
            case 1: {
                typeOfLog = "CHANNEL_REQUEST";
                break;
            }
            case 2: {
                typeOfLog = "CHANNEL_RESPONSE";
                break;
            }
            case 3: {
                typeOfLog = "ADAPTOR_RESPONSE";
                break;
            }
            case 4: {
                typeOfLog = "FROM_CORE";
                break;
            }
            case 5: {
                typeOfLog = "APPLICATION_ERRORS";
                break;
            }
            case 6: {
                typeOfLog = "DATABASE";
                break;
            }
            case 7: {
                typeOfLog = "LOGIN";
                break;
            }
            case 13: {
                typeOfLog = "FROM_RESPONSE_QUEUE_ADAPTORS";
                break;
            }
            case 14: {
                typeOfLog = "TO_AIRTIME_QUEUE_ADAPTOR";
                break;
            }
            case 15: {
                typeOfLog = "TO_RESPONSE_QUEUE_ADAPTORS";
                break;
            }
            case 16: {
                typeOfLog = "TO_NOTIFICATION_QUEUE";
                break;
            }
            case 20: {
                typeOfLog = "CHANNEL_TRAN_NOTIFICATIONS";
                break;
            }
            case 21: {
                typeOfLog = "TO_SMS_GATEWAY";
                break;
            }
            case 22: {
                typeOfLog = "FROM_SMS_GATEWAY";
                break;
            }
            case 23: {
                typeOfLog = "TO_AIRTIME_GATEWAY";
                break;
            }
            case 24: {
                typeOfLog = "FROM_AIRTIME_GATEWAY";
                break;
            }
            case 52: {
                typeOfLog = "TO_CBS";
                break;
            }
            case 53: {
                typeOfLog = "FROM_CBS";
                break;
            }
            default: {
                typeOfLog = "Others";
            }
        }
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String LogDate = formatter.format(today);
        SimpleDateFormat LogTimeformatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String LogTime = LogTimeformatter.format(today);
        File dir = new File(LOGS_PATH + "/" + LogDate + "/" + typeOfLog);
        BufferedWriter writer = null;
        if (dir.exists()) {
            dir.setWritable(true);
        } else {
            dir.mkdirs();
            dir.setWritable(true);
        }
        try {
            SimpleDateFormat formatterLog = new SimpleDateFormat("HHmm");
            Date todaysDate = new Date();
            Calendar calendars = Calendar.getInstance();
            calendars.setTime(todaysDate);
            int unroundedMinutes = calendars.get(12);
            int mod = unroundedMinutes % 5;
            calendars.add(12, mod < 8 ? -mod : 5 - mod);
            Date roundOfTime = calendars.getTime();
            String fileName = "/" + LogDate + "-" + formatterLog.format(roundOfTime) + ".log";
            writer = new BufferedWriter(new FileWriter(dir + fileName, true));
            writer.write(LogTime + " ~ " + details);
            writer.newLine();
        } catch (IOException e) {
            Logger.getLogger(ServiceLogger.class.getName()).log(Level.SEVERE, "ERROR: Failed to load properties file.\nCause: \n", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                Logger.getLogger(ServiceLogger.class.getName()).log(Level.SEVERE, "ERROR: Failed to load properties file.\nCause: \n", e);
            }
        }
    }

    public static String logPreString() {
        return "ESB CORE | " + Thread.currentThread().getStackTrace()[2].getClassName() + " | " + Thread.currentThread().getStackTrace()[2].getLineNumber() + " | " + Thread.currentThread().getStackTrace()[2].getMethodName() + "() | ";
    }
}
