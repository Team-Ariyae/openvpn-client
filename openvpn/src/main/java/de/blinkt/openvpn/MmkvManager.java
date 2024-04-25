package de.blinkt.openvpn;

import com.tencent.mmkv.MMKV;

public final class MmkvManager {
    private static MMKV connectionStorage;
    private static MMKV logStorage;

    private static final String ID_connection_data = "ccS_Java";
    private static final String ID_log_data = "ldd_Java";

    public static synchronized MMKV getConnectionStorage() {
        if (connectionStorage == null) {
            connectionStorage = MMKV.mmkvWithID(ID_connection_data, MMKV.MULTI_PROCESS_MODE);
        }
        return connectionStorage;
    }
    public static synchronized MMKV getLogStorage() {
        if (logStorage == null) {
            logStorage = MMKV.mmkvWithID(ID_log_data, MMKV.MULTI_PROCESS_MODE);
        }
        return logStorage;
    }

}
