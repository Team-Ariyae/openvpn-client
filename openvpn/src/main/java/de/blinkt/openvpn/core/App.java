package de.blinkt.openvpn.core;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Keep;

import java.util.ArrayList;

/**
 * MehrabSp
 * This is init file for OpenVpn client
 */
@Keep
public class App { // extends /*com.orm.SugarApp*/ Application
    public static String ContentTitle = "OpenVpn"; // Notif title
    static NotificationManager manager;
    static ArrayList<String> appsList = new ArrayList<>(); // SplitTunnel Apps
    public static Context contextApplication = null;
    public static Boolean isShowToast = false;

    public static void setOpenVpn(Context context, String channelID, String channelIDName) {
        setOpenVpn(context, channelID, channelIDName, ContentTitle, false);
    }

    /**
     * by MehrabSp
     * @param context ApplicationContext
     * @param channelID Example: com.example.app
     * @param channelIDName Example: comexampleapp
     * @param contentTitle Example: iNetVpn
     */
    public static void setOpenVpn(Context context, String channelID, String channelIDName,
                                  String contentTitle, Boolean showToast) {

        if (contentTitle.isEmpty() || channelID.isEmpty() || channelIDName.isEmpty()) {
            throw new RuntimeException("OpenVPN Configuration must be have params");
        }
        try{
            createNotificationChannel(context, channelID, channelIDName);
            ContentTitle = contentTitle;

            PRNGFixes.apply();
            StatusListener mStatus = new StatusListener();
            mStatus.init(context);
            contextApplication = context;

            isShowToast = showToast;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * by MehrabSp
     * @param packageName Example: com.android.chrome
     */
    @Keep
    public static void addDisallowedPackageApplication(String packageName){
        appsList.add(packageName);
    }

    public static void clearDisallowedPackageApplication(){
        appsList.clear();
    }

    public static void removeDisallowedPackageApplication(String packageName){
        appsList.remove(packageName);
    }

    public static void addArrayDisallowedPackageApplication(ArrayList<String> packageList){
        appsList.addAll(packageList);
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
