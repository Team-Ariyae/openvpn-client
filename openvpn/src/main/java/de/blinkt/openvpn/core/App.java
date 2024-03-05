package de.blinkt.openvpn.core;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Keep;

/**
 * MehrabSp
 * This is init file for OpenVpn client
 */
@Keep
public class App { // extends /*com.orm.SugarApp*/ Application
//    public static String CHANNEL_ID = ""; // Example: com.example.app
//    public static String CHANNEL_ID_NAME = ""; // Example: comexampleapp
    public static String ContentTitle = "OpenVpn"; // OpenVpn (Show in notif title)

    static NotificationManager manager;

    @Keep
    public static void setOpenVpn(Context context, String channelID, String channelIDName, String contentTitle) {

        if (contentTitle.isEmpty() || channelID.isEmpty() || channelIDName.isEmpty()) {
            throw new RuntimeException("OpenVPN Configuration must be have params");
        }
        createNotificationChannel(context, channelID, channelIDName);

        ContentTitle = contentTitle;
//        CHANNEL_ID = channelID;
//        CHANNEL_ID_NAME = channelIDName;

        PRNGFixes.apply();
        StatusListener mStatus = new StatusListener();
        mStatus.init(context);
    }

    private static void createNotificationChannel(Context context, String channelID, String channelIDName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        channelID,
                        channelIDName,
                        NotificationManager.IMPORTANCE_LOW
                );

                serviceChannel.setSound(null, null);
                manager = context.getSystemService(NotificationManager.class);
                manager.createNotificationChannel(serviceChannel);
            }
        } catch (Exception e) {
            Log.e("createNotifiChannel", String.valueOf(e));
            throw new RuntimeException("You have error in [createNotificationChannel]");
        }
    }

}
