package io.sugo.android.mpmetrics;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Abstracts away possibly non-present system information classes,
 * and handles permission-dependent queries for default system information.
 */
/* package */ class SystemInformation {

    public SystemInformation(Context context) {
        mContext = context;

        PackageManager packageManager = mContext.getPackageManager();

        String foundAppVersionName = null;
        Integer foundAppVersionCode = null;
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
            foundAppVersionName = packageInfo.versionName;
            foundAppVersionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            Log.w(LOGTAG, "System information constructed with a context that apparently doesn't exist.");
        }

        mAppVersionName = foundAppVersionName;
        mAppVersionCode = foundAppVersionCode;

        // We can't count on these features being available, since we need to
        // run on old devices. Thus, the reflection fandango below...
        Class<? extends PackageManager> packageManagerClass = packageManager.getClass();

        Method hasSystemFeatureMethod = null;
        try {
            hasSystemFeatureMethod = packageManagerClass.getMethod("hasSystemFeature", String.class);
        } catch (NoSuchMethodException e) {
            // Nothing, this is an expected outcome
        }

        Boolean foundNFC = null;
        Boolean foundTelephony = null;
        if (null != hasSystemFeatureMethod) {
            try {
                foundNFC = (Boolean) hasSystemFeatureMethod.invoke(packageManager, "android.hardware.nfc");
                foundTelephony = (Boolean) hasSystemFeatureMethod.invoke(packageManager, "android.hardware.telephony");
            } catch (InvocationTargetException e) {
                Log.w(LOGTAG, "System version appeared to support PackageManager.hasSystemFeature, but we were unable to call it.");
            } catch (IllegalAccessException e) {
                Log.w(LOGTAG, "System version appeared to support PackageManager.hasSystemFeature, but we were unable to call it.");
            }
        }

        mHasNFC = foundNFC;
        mHasTelephony = foundTelephony;
        mDisplayMetrics = new DisplayMetrics();

        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(mDisplayMetrics);
    }

    public String getAppVersionName() {
        return mAppVersionName;
    }

    public Integer getAppVersionCode() {
        return mAppVersionCode;
    }

    public boolean hasNFC() {
        return mHasNFC;
    }

    public boolean hasTelephony() {
        return mHasTelephony;
    }

    public DisplayMetrics getDisplayMetrics() {
        return mDisplayMetrics;
    }

    public String getPhoneRadioType() {
        String ret = null;

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != telephonyManager) {
            switch (telephonyManager.getPhoneType()) {
                case 0x00000000: // TelephonyManager.PHONE_TYPE_NONE
                    ret = "none";
                    break;
                case 0x00000001: // TelephonyManager.PHONE_TYPE_GSM
                    ret = "gsm";
                    break;
                case 0x00000002: // TelephonyManager.PHONE_TYPE_CDMA
                    ret = "cdma";
                    break;
                case 0x00000003: // TelephonyManager.PHONE_TYPE_SIP
                    ret = "sip";
                    break;
                default:
                    ret = null;
            }
        }

        return ret;
    }

    public String getDeviceId() {
        String deviceId = null;
        try {
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            deviceId = tm.getDeviceId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (TextUtils.isEmpty(deviceId)) {
                WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                String mac = wifi.getConnectionInfo().getMacAddress();
                deviceId = mac;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = Settings.Secure.getString(mContext.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deviceId;
    }

    // Note this is the *current*, not the canonical network, because it
    // doesn't require special permissions to access. Unreliable for CDMA phones,
    //
    public String getCurrentNetworkOperator() {
        String ret = null;

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != telephonyManager) {
            ret = telephonyManager.getNetworkOperatorName();
        }

        return ret;
    }

    public Boolean isWifiConnected() {
        Boolean ret = null;

        if (PackageManager.PERMISSION_GRANTED == mContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            ret = (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected());
        }

        return ret;
    }

    /**
     * 当前网络类型
     *
     * @return 2g / 3g / 4g / wifi / unknown
     */
    public String getNetworkType() {

        final String NETWORK_2_G = "2g";
        final String NETWORK_3_G = "3g";
        final String NETWORK_4_G = "4g";
        final String NETWORK_WIFI = "wifi";
        final String NETWORK_UNKNOWN = "unknown";

        String ret = NETWORK_UNKNOWN;

        if (PackageManager.PERMISSION_GRANTED == mContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            //NetworkInfo对象为空 则代表没有网络
            if (networkInfo == null) {
                return null;
            }
            int nType = networkInfo.getType();
            if (nType == ConnectivityManager.TYPE_WIFI) {
                ret = NETWORK_WIFI;
            } else if (nType == ConnectivityManager.TYPE_MOBILE) {
                int networkType = networkInfo.getSubtype();
                switch (networkType) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case 16:        // NETWORK_TYPE_GSM:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        ret = NETWORK_2_G;
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                    case 17:        // NETWORK_TYPE_TD_SCDMA:
                        ret = NETWORK_3_G;
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                    case 18:        // NETWORK_TYPE_IWLAN:
                        ret = NETWORK_4_G;
                        break;
                    default:
                        return NETWORK_UNKNOWN;
                }
            }
        }
        return ret;
    }

    public Boolean isBluetoothEnabled() {
        Boolean isBluetoothEnabled = null;
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                isBluetoothEnabled = bluetoothAdapter.isEnabled();
            }
        } catch (SecurityException e) {
            // do nothing since we don't have permissions
        } catch (NoClassDefFoundError e) {
            // Some phones doesn't have this class. Just ignore it
        }
        return isBluetoothEnabled;
    }

    public String getBluetoothVersion() {
        String bluetoothVersion = null;
        if (android.os.Build.VERSION.SDK_INT >= 8) {
            bluetoothVersion = "none";
            if (android.os.Build.VERSION.SDK_INT >= 18 &&
                    mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                bluetoothVersion = "ble";
            } else if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                bluetoothVersion = "classic";
            }
        }
        return bluetoothVersion;
    }

    private final Context mContext;

    // Unchanging facts
    private final Boolean mHasNFC;
    private final Boolean mHasTelephony;
    private final DisplayMetrics mDisplayMetrics;
    private final String mAppVersionName;
    private final Integer mAppVersionCode;

    private static final String LOGTAG = "SugoAPI.SysInfo";
}
