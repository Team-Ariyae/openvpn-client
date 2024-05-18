package com.lazycoder.cakevpn.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.lazycoder.cakevpn.CheckInternetConnection;
import com.lazycoder.cakevpn.R;
import com.lazycoder.cakevpn.SharedPreference;
import com.lazycoder.cakevpn.databinding.FragmentMainBinding;
import com.lazycoder.cakevpn.interfaces.ChangeServer;
import com.lazycoder.cakevpn.model.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import sp.de.blinkt.openvpn.OpenVpnApi;
import sp.de.blinkt.openvpn.core.OpenVPNService;
import sp.de.blinkt.openvpn.core.OpenVPNThread;
import sp.de.blinkt.openvpn.core.VpnStatus;

import static android.app.Activity.RESULT_OK;

public class MainFragment extends Fragment implements View.OnClickListener, ChangeServer {

    private Server server;
    private CheckInternetConnection connection;

    private OpenVPNThread vpnThread = new OpenVPNThread();
    private OpenVPNService vpnService = new OpenVPNService();
    boolean vpnStart = false;
    private SharedPreference preference;

    private FragmentMainBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);

        View view = binding.getRoot();
        initializeAll();

        return view;
    }

    /**
     * Initialize all variable and object
     */
    private void initializeAll() {
        preference = new SharedPreference(getContext());
        server = preference.getServer();

        // Update current selected server icon
        updateCurrentServerIcon(server.getFlagUrl());

        connection = new CheckInternetConnection();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.vpnBtn.setOnClickListener(this);

        // Checking is vpn already running or not
        isServiceRunning();
        VpnStatus.initLogCache(getActivity().getCacheDir());
    }

    /**
     * @param v: click listener view
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.vpnBtn) {// Vpn is running, user would like to disconnect current connection.
            if (vpnStart) {
                confirmDisconnect();
            } else {
                prepareVpn();
            }
        }
    }

    /**
     * Show show disconnect confirm dialog
     */
    public void confirmDisconnect(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getString(R.string.connection_close_confirm));

        builder.setPositiveButton(getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                stopVpn();
            }
        });
        builder.setNegativeButton(getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Prepare for vpn connect with required permission
     */
    private void prepareVpn() {
        if (!vpnStart) {
            if (getInternetStatus()) {

                // Checking permission for network monitor
                Intent intent = VpnService.prepare(getContext());

                if (intent != null) {
                    startActivityForResult(intent, 1);
                } else startVpn();//have already permission

                // Update confection status
                status("connecting");

            } else {

                // No internet connection available
                showToast("you have no internet connection !!");
            }

        } else if (stopVpn()) {

            // VPN is stopped, show a Toast message.
            showToast("Disconnect Successfully");
        }
    }

    /**
     * Stop vpn
     * @return boolean: VPN status
     */
    public boolean stopVpn() {
        try {
            vpnThread.stop();

            status("connect");
            vpnStart = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Taking permission for network access
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            //Permission granted, start the VPN
            startVpn();
        } else {
            showToast("Permission Deny !! ");
        }
    }

    /**
     * Internet connection status.
     */
    public boolean getInternetStatus() {
        return connection.netCheck(getContext());
    }

    /**
     * Get service status
     */
    public void isServiceRunning() {
        setStatus(vpnService.getStatus());
    }

    /**
     * Start the VPN
     */
    private void startVpn() {
        try {
            showToast("Started!");

            String config = """
key-direction 1
auth-user-pass
client
proto tcp-client
remote 78.39.51.29 9987
dev tun
resolv-retry infinite
nobind
persist-key
persist-tun
remote-cert-tls server
verify-x509-name server_Qc4gab3Cs7LvQ2sh name
auth SHA256
auth-nocache
cipher AES-128-GCM
tls-client
tls-version-min 1.2
tls-cipher TLS-ECDHE-ECDSA-WITH-AES-128-GCM-SHA256
ignore-unknown-option block-outside-dns
setenv opt block-outside-dns # Prevent Windows 10 DNS leak
verb 3
<ca>
-----BEGIN CERTIFICATE-----
MIIB1zCCAX2gAwIBAgIUavU+CAVM6SSbiTFxUkZBprjyMY4wCgYIKoZIzj0EAwIw
HjEcMBoGA1UEAwwTY25fbmdjUUFyWjZxUDBNb1RIZDAeFw0yNDA0MDIyMTAzMDZa
Fw0zNDAzMzEyMTAzMDZaMB4xHDAaBgNVBAMME2NuX25nY1FBclo2cVAwTW9USGQw
WTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQIgDf52aQ2CkJ+GftJrgFTbOoFaaHv
n1C24bNkkZCZLDKsMTTcDWpKJ/nEYpqApPZ/9t4+rQozpwTbnM0JscRgo4GYMIGV
MAwGA1UdEwQFMAMBAf8wHQYDVR0OBBYEFH8W4lqGQEd255+laLP75gGPKhEOMFkG
A1UdIwRSMFCAFH8W4lqGQEd255+laLP75gGPKhEOoSKkIDAeMRwwGgYDVQQDDBNj
bl9uZ2NRQXJaNnFQME1vVEhkghRq9T4IBUzpJJuJMXFSRkGmuPIxjjALBgNVHQ8E
BAMCAQYwCgYIKoZIzj0EAwIDSAAwRQIhAKa8I7r2T9QWI3NpWgZwp8wtvD4a0YqF
ciT+5KFp7RkxAiAgPaXZz2XxStwUVB2jWj/4SCuB3xHSSPIc85fuqWcpoA==
-----END CERTIFICATE-----
</ca>
<cert>
-----BEGIN CERTIFICATE-----
MIIB1jCCAXugAwIBAgIQTLrIkagoN1i7/ASYQhkzpTAKBggqhkjOPQQDAjAeMRww
GgYDVQQDDBNjbl9uZ2NRQXJaNnFQME1vVEhkMB4XDTI0MDQwMjIxMDcwNVoXDTI2
MDcwNjIxMDcwNVowDjEMMAoGA1UEAwwDbW1kMFkwEwYHKoZIzj0CAQYIKoZIzj0D
AQcDQgAEK9xSxM9Ri/5wcf3a2BwFWjwhghlhwKghpHpV5+H5nlueGCIHE3+w6FqZ
c4M2J7RFyJff8ezmC/nB/4eXHlocw6OBqjCBpzAJBgNVHRMEAjAAMB0GA1UdDgQW
BBRpHr0l3Qi0IYt5RAH6wGIAS+dcXDBZBgNVHSMEUjBQgBR/FuJahkBHduefpWiz
++YBjyoRDqEipCAwHjEcMBoGA1UEAwwTY25fbmdjUUFyWjZxUDBNb1RIZIIUavU+
CAVM6SSbiTFxUkZBprjyMY4wEwYDVR0lBAwwCgYIKwYBBQUHAwIwCwYDVR0PBAQD
AgeAMAoGCCqGSM49BAMCA0kAMEYCIQDIleKm+7Auegyipu50h3OyAgCleJ0cYPk9
ytiXC/DO8wIhAP/WsHjWphBDW7KBVsXVYa+CbeH9gUfFfKwtZWD7H6Zz
-----END CERTIFICATE-----
</cert>
<key>
-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgI24ntnwYjYZ0CxNd
gB7jFIvQGijJeZUDL4n0PMst1M6hRANCAAQr3FLEz1GL/nBx/drYHAVaPCGCGWHA
qCGkelXn4fmeW54YIgcTf7DoWplzgzYntEXIl9/x7OYL+cH/h5ceWhzD
-----END PRIVATE KEY-----
</key>
<tls-crypt>
#
# 2048 bit OpenVPN static key
#
-----BEGIN OpenVPN Static key V1-----
4f47df5d26cdbdf0494a779eaaadc497
e932c4d16699c5438f5fbd9627a8500b
34c6878655962172d61036cb5faf2800
2f93b06e67b12b55fffa34e2d6a53d86
4f9d0d3a678efb7e336c79720ef5e98d
80ef629742c67fe8ceb914fb5e08c697
8f5d815167db0bf8a22952d055488d11
d41782e75100c3bd23f06ce936a5447c
fb18b240d5a93be931806cb15debb314
e1732b062a53d64f95003dcc28c17d62
0fe5c5ce62d0fa67ef34f5295a351eec
530d2f41e091259401c2737606ae9598
52e2c9e5c050fde7479cee9b14ecd23b
c24b64bcb3e57ae4a9953b420d9b4530
38e930e3c226294afd513e329f99d175
3076b0bb687d9bc6ca91308484836590
-----END OpenVPN Static key V1-----
</tls-crypt>
""";
            Log.d("OP", config);

            OpenVpnApi.startVpn(getContext(), config, "Sweden", "My", "My");

            // Update log
            binding.logTv.setText("Connecting...");
            vpnStart = true;

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Status change with corresponding vpn connection status
     * @param connectionState
     */
    public void setStatus(String connectionState) {
        if (connectionState!= null)
            switch (connectionState) {
            case "DISCONNECTED":
                status("connect");
                vpnStart = false;
                vpnService.setDefaultStatus();
                binding.logTv.setText("");
                break;
            case "CONNECTED":
                vpnStart = true;// it will use after restart this activity
                status("connected");
                binding.logTv.setText("");
                break;
            case "WAIT":
                binding.logTv.setText("waiting for server connection!!");
                break;
            case "AUTH":
                binding.logTv.setText("server authenticating!!");
                break;
            case "RECONNECTING":
                status("connecting");
                binding.logTv.setText("Reconnecting...");
                break;
            case "NONETWORK":
                binding.logTv.setText("No network connection");
                break;
        }

    }

    /**
     * Change button background color and text
     * @param status: VPN current status
     */
    public void status(String status) {

        if (status.equals("connect")) {
            binding.vpnBtn.setText(getContext().getString(R.string.connect));
        } else if (status.equals("connecting")) {
            binding.vpnBtn.setText(getContext().getString(R.string.connecting));
        } else if (status.equals("connected")) {

            binding.vpnBtn.setText(getContext().getString(R.string.disconnect));

        } else if (status.equals("tryDifferentServer")) {

            binding.vpnBtn.setBackgroundResource(R.drawable.button_connected);
            binding.vpnBtn.setText("Try Different\nServer");
        } else if (status.equals("loading")) {
            binding.vpnBtn.setBackgroundResource(R.drawable.button);
            binding.vpnBtn.setText("Loading Server..");
        } else if (status.equals("invalidDevice")) {
            binding.vpnBtn.setBackgroundResource(R.drawable.button_connected);
            binding.vpnBtn.setText("Invalid Device");
        } else if (status.equals("authenticationCheck")) {
            binding.vpnBtn.setBackgroundResource(R.drawable.button_connecting);
            binding.vpnBtn.setText("Authentication \n Checking...");
        }

    }

    /**
     * Receive broadcast message
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                setStatus(intent.getStringExtra("state"));
            } catch (Exception e) {
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
                e.printStackTrace();
            }

        }
    };

    /**
     * Update status UI
     * @param duration: running time
     * @param lastPacketReceive: last packet receive time
     * @param byteIn: incoming data
     * @param byteOut: outgoing data
     */
    public void updateConnectionStatus(String duration, String lastPacketReceive, String byteIn, String byteOut) {
        binding.durationTv.setText("Duration: " + duration);
        binding.lastPacketReceiveTv.setText("Packet Received: " + lastPacketReceive + " second ago");
        binding.byteInTv.setText("Bytes In: " + byteIn);
        binding.byteOutTv.setText("Bytes Out: " + byteOut);
    }

    /**
     * Show toast message
     * @param message: toast message
     */
    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * VPN server country icon change
     * @param serverIcon: icon URL
     */
    public void updateCurrentServerIcon(String serverIcon) {
        Glide.with(getContext())
                .load(serverIcon)
                .into(binding.selectedServerIcon);
    }

    /**
     * Change server when user select new server
     * @param server ovpn server details
     */
    @Override
    public void newServer(Server server) {
        this.server = server;
        updateCurrentServerIcon(server.getFlagUrl());

        // Stop previous connection
        if (vpnStart) {
            stopVpn();
        }

        prepareVpn();
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter("connectionState"));

        if (server == null) {
            server = preference.getServer();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    /**
     * Save current selected server on local shared preference
     */
    @Override
    public void onStop() {
        if (server != null) {
            preference.saveServer(server);
        }

        super.onStop();
    }
}
