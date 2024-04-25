package de.blinkt.openvpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import de.blinkt.openvpn.core.App;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;
import de.blinkt.openvpn.core.VpnStatus;

import com.tencent.mmkv.MMKV;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * by MehrabSp
 * Static all
 */
public final class VPNManager {

    public interface StatusCallback {
        void onStatusResult(String status);
        void onStatusResult(String status, Boolean err, String errmsg);
    }

    public static OpenVPNThread vpnThread = new OpenVPNThread();
    private static OpenVPNService vpnService = new OpenVPNService();
    private static boolean vpnStart = false;
    private static StatusCallback statusCallback;

    @NotNull
    private static final Context contextApplication = App.contextApplication;

    /**
     * run on activity!
     */
    public void onCreateView() {
        onCreateView(false);
    }
    public void onCreateView(Boolean initMmkv) {
        initializeMMKV(initMmkv);

        // Checking is vpn already running or not
        isServiceRunning();
        VpnStatus.initLogCache(contextApplication.getCacheDir());
    }

    /**
     * Initialize all variable and object
     */
    private void initializeMMKV(Boolean initMmkv) {
        if(initMmkv)
            MMKV.initialize(contextApplication);
    }

    /**
     * Status
     * @param callback Status with callback
     */
    public static void setRetCallBackStatus(StatusCallback callback) {
        statusCallback = callback;
    }

    private static void sendStatusToCallBack(String str){
        statusCallback.onStatusResult(str, false, null);
    }
    private static void sendStatusToCallBack(String str, Boolean err, String errmsg){
        statusCallback.onStatusResult(str, err, errmsg);
    }

    /**
     * @param v: click listener view
     */
    public void onClick(@NotNull String v) {
        if (Objects.equals(v, "force_stop")) {// Vpn is running, user would like to
            // disconnect current connection.
            stopVpn();
        }
    }

    public void onClick(ComponentActivity componentActivity, @NotNull String v,
                        @NotNull String config, String password,
                        String username) {
        if (Objects.equals(v, "click")) {// Vpn is running, user would like to disconnect current
            // connection.
            if (vpnStart) {
                stopVpn();
            } else {
                prepareVpn(componentActivity, config, password, username);
            }
        } else if (Objects.equals(v, "force_start")) {// Vpn is running, user would like to
            // disconnect current
            // connection.
            if (stopVpn()) {
                // VPN is stopped, show a Toast message.
                showToast("Disconnect Successfully");
            }

            prepareVpn(componentActivity, config, password, username);

        }
    }

    /**
     * Prepare for vpn connect with required permission
     */
    private static void prepareVpn(ComponentActivity componentActivity, String config,
                                   String password,
                                   String username) {
        if (!vpnStart) {
            // Checking permission for network monitor
            Intent intent = VpnService.prepare(contextApplication);

            if (intent != null) {
                try{
                    componentActivity.startActivityForResult(intent, 1);
                }catch (Exception e){
                    sendStatusToCallBack("VPNSERVICE", true, e.toString());
                    e.printStackTrace();
                }
            } else startVpn(config, password, username);//have already permission

        } else if (stopVpn()) {

            // VPN is stopped, show a Toast message.
            showToast("Disconnect Successfully");
        }
    }

    /**
     * Stop vpn
     *
     * @return boolean: VPN status
     */
    public static boolean stopVpn() {
        try {
            OpenVPNThread.stop();

            vpnStart = false;
            return true;
        } catch (Exception e) {
            sendStatusToCallBack("STOPTHREAD", true, e.toString());
        }
        return false;
    }

    /**
     * Get service status
     */
    public void isServiceRunning() {
        setStatus(OpenVPNService.getStatus());
    }

    /**
     * Start the VPN
     */
    public static void startVpn(@NotNull String config, String password, String username) {
        try {
            showToast("Started!");

            OpenVpnApi.startVpn(contextApplication, config, "Sweden", username, password);

            vpnStart = true;

        } catch (RemoteException e) {
            sendStatusToCallBack("START", true, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Status change with corresponding vpn connection status
     *
     * @param connectionState msg
     */
    public static void setStatus(String connectionState) {
        if (connectionState != null)
            sendStatusToCallBack(connectionState);
    }

    /**
     * Receive broadcast message
     */
    private static BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                setStatus(intent.getStringExtra("state"));
            } catch (Exception e) {
                sendStatusToCallBack("SENDSTATUS", true, e.toString());
                e.printStackTrace();
            }

            try {

                String duration = intent.getStringExtra("duration");
                String lastPacketReceive = intent.getStringExtra("lastPacketReceive");
                String byteIn = intent.getStringExtra("byteIn");
                String byteOut = intent.getStringExtra("byteOut");

                if (duration == null) duration = "00:00:00";
                if (lastPacketReceive == null) lastPacketReceive = "0";
                if (byteIn == null) byteIn = " ";
                if (byteOut == null) byteOut = " ";
                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut);
            } catch (Exception e) {
                sendStatusToCallBack("STATUS", true, e.toString());
                e.printStackTrace();
            }

        }
    };

    /**
     * Update status UI
     *
     * @param duration:          running time
     * @param lastPacketReceive: last packet receive time
     * @param byteIn:            incoming data
     * @param byteOut:           outgoing data
     */
    public static void updateConnectionStatus(String duration, String lastPacketReceive, String byteIn, String byteOut) {
//        binding.durationTv.setText("Duration: " + duration);
//        binding.lastPacketReceiveTv.setText("Packet Received: " + lastPacketReceive + " second ago");
//        binding.byteInTv.setText("Bytes In: " + byteIn);
//        binding.byteOutTv.setText("Bytes Out: " + byteOut);
    }

    /**
     * Show toast message
     *
     * @param message: toast message
     */
    public static void showToast(String message) {
        if(App.isShowToast)
            Toast.makeText(contextApplication, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Change server when user select new server
     */
    public static void newServer(ComponentActivity componentActivity, String config,
                                 String password, String username) {
        // Stop previous connection
        if (vpnStart) {
            stopVpn();
        }

        prepareVpn(componentActivity, config, password, username);
    }


    public static void onResume() {
        LocalBroadcastManager.getInstance(contextApplication).registerReceiver(broadcastReceiver,
                new IntentFilter("connectionState"));

        vpnStart = MmkvManager.getConnectionStorage().getBoolean("vpnStart", vpnStart);
    }

    public static void onPause() {
        LocalBroadcastManager.getInstance(contextApplication).unregisterReceiver(broadcastReceiver);
    }

    /**
     * Save current status
     */
    public static void onStop() {
        MmkvManager.getConnectionStorage().putBoolean("vpnStart", vpnStart);
    }

}
