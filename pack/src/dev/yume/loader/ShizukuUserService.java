package dev.yume.loader;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Shizuku user service used for the short XMSF firewall bypass around island updates.
 *
 * <p>Shizuku starts a user service in its own process (an {@code app_process} running as shell or
 * root) and loads the requested class from the client's own DEX files. This class therefore lives in
 * the loader DEX, which is the APK's {@code classes.dex} and stays readable for the shell user. A
 * class inside the packed payload cannot be used here: the payload is extracted below
 * {@code /data/user/0/<pkg>}, which the shell user cannot traverse.</p>
 *
 * <p>The Binder contract has to stay in sync with
 * {@code com.github.yumeyucca.yumebox.runtime.service.shizuku.IPrivilegedService}, and the firewall
 * call below with {@code ...shizuku.OemDenyFirewall} (this class cannot share that code: it runs
 * from the APK's loader DEX).</p>
 */
public final class ShizukuUserService extends Binder {

    private static final String TAG = "YumeBoxShizukuService";

    private static final String DESCRIPTOR =
            "com.github.yumeyucca.yumebox.runtime.service.shizuku.IPrivilegedService";

    private static final int TRANSACTION_SET_PACKAGE_NETWORKING_ENABLED = FIRST_CALL_TRANSACTION;

    private static final int FIREWALL_CHAIN_OEM_DENY = 9;

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            if (reply != null) {
                reply.writeString(DESCRIPTOR);
            }
            return true;
        }
        if (code != TRANSACTION_SET_PACKAGE_NETWORKING_ENABLED) {
            return super.onTransact(code, data, reply, flags);
        }

        data.enforceInterface(DESCRIPTOR);
        int uid = data.readInt();
        boolean enabled = data.readInt() != 0;
        boolean result = setPackageNetworkingEnabled(uid, enabled);
        if (reply != null) {
            reply.writeNoException();
            reply.writeInt(result ? 1 : 0);
        }
        return true;
    }

    /**
     * Adds or removes the OEM deny firewall rule of {@code uid} through the hidden connectivity API.
     * Both calls run as the user service, i.e. with the privileges of the Shizuku server.
     */
    private static boolean setPackageNetworkingEnabled(int uid, boolean enabled) {
        try {
            ReflectionAccess.exemptHiddenApis();
        } catch (Throwable error) {
            Log.w(TAG, "Unable to exempt hidden APIs", error);
        }

        try {
            Method getService = Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String.class);
            IBinder connectivityBinder = (IBinder) getService.invoke(null, "connectivity");
            if (connectivityBinder == null) {
                Log.e(TAG, "Connectivity service is not available");
                return false;
            }

            Object connectivityManager = Class.forName("android.net.IConnectivityManager$Stub")
                    .getMethod("asInterface", IBinder.class)
                    .invoke(null, connectivityBinder);

            connectivityManager.getClass()
                    .getMethod("setFirewallChainEnabled", int.class, boolean.class)
                    .invoke(connectivityManager, FIREWALL_CHAIN_OEM_DENY, true);

            connectivityManager.getClass()
                    .getMethod("setUidFirewallRule", int.class, int.class, int.class)
                    .invoke(connectivityManager, FIREWALL_CHAIN_OEM_DENY, uid, enabled ? 0 : 2);

            Log.d(TAG, "Networking of uid " + uid + " enabled: " + enabled);
            return true;
        } catch (Throwable error) {
            Log.e(TAG, "Failed to update networking of uid " + uid, error);
            return false;
        }
    }
}
