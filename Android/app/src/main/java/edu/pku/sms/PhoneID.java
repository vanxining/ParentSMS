package edu.pku.sms;

import android.telephony.TelephonyManager;
import android.util.Log;

class PhoneID {
    private static String phoneID = "0000";

    static String getPhoneID(TelephonyManager tm) {
        if (tm == null) {
            return phoneID;
        }

        phoneID = doGetPhoneID(tm);
        return phoneID;
    }

    private static String doGetPhoneID(TelephonyManager tm) {
        String imei = tm.getDeviceId();
        String number = tm.getLine1Number();

        Log.i("PhoneID", "IMEI: " + imei);
        Log.i("PhoneID", "Phone number: " + number);

        if (number == null || number.isEmpty()) {
            if (imei == null || imei.isEmpty()) {
                return phoneID;
            }

            return imei;
        }

        if (number.startsWith("+86")) {
            return number.substring(3);
        }

        return number;
    }
}
