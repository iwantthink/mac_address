package com.hypers.www.demomacaddress;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Context mContext;
    private TextView mTvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = MainActivity.this;
        mTvInfo = (TextView) findViewById(R.id.tv_info);
        mTvInfo.setMovementMethod(ScrollingMovementMethod.getInstance());
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTvInfo.setText("mac address :" + getMacAddress(mContext) + "\n");
                Log.d("MainActivity", "mac address =" + getMacAddress(mContext));
                Log.d("MainActivity", "file = " + getAddressMacByFile(mContext));
                Log.d("MainActivity", "interface =" + getAdressMacByInterface(mContext));
                Log.d(TAG, "cat = " + getMacAddressAfterSDK23());
            }
        });
    }

    private String getWifiMac() {
        WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        String macAddress = manager.getConnectionInfo().getMacAddress();
        return macAddress;
    }

    private String getWifiMacByJava() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nIF = interfaces.nextElement();
                byte[] addr = nIF.getHardwareAddress();
                if (addr == null || addr.length == 0) {
                    continue;
                }
                StringBuilder buf = new StringBuilder();
                for (byte b : addr) {
                    buf.append(String.format("%02X:", b));
                }
                if (buf.length() > 0) {
                    buf.deleteCharAt(buf.length() - 1);
                }

                String mac = buf.toString();
                stringBuilder.append("interfaceName=" + nIF.getName() + ", mac=" + mac + "\n");
                Log.d("mac", "interfaceName=" + nIF.getName() + ", mac=" + mac);
            }
        } catch (Exception e) {
            Log.e("MainActivity", e.getMessage());
        }
        return stringBuilder.toString();
    }


    private static final String MARSHMALLOW_DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
    private static final String SYS_CLASS_NET = "/sys/class/net/";
    private static final String SYS_CLASS_NET_SUFFIX = "/address";

    /**
     * 获取mac
     *
     * @param context
     * @return
     */
    public static String getMacAddress(Context context) {
        String wifimac = "";
        if (checkPermissions(context, "android.permission.ACCESS_WIFI_STATE")) {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo.getMacAddress().equals(MARSHMALLOW_DEFAULT_MAC_ADDRESS)) {
                String result = getAdressMacByInterface(context);
                if (!TextUtils.isEmpty(result)) {
                    return result.toLowerCase();
                }
                result = getAddressMacByFile(context);
                if (!TextUtils.isEmpty(result)) {
                    return result.toLowerCase();
                }
                return MARSHMALLOW_DEFAULT_MAC_ADDRESS;
            } else {
                return wifiInfo.getMacAddress().toLowerCase();
            }
        } else {
            Log.e(TAG, "need permission :android.permission.ACCESS_WIFI_STATE");
        }
        return wifimac;
    }


    private static String getAdressMacByInterface(Context context) {
        if (checkPermissions(context, "android.permission.INTERNET")) {
            try {
                List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface nif : all) {
                    if (nif.getName().equalsIgnoreCase("wlan0")) {
                        byte[] macBytes = nif.getHardwareAddress();
                        if (macBytes == null) {
                            return "";
                        }
                        StringBuilder res1 = new StringBuilder();
                        for (byte b : macBytes) {
                            res1.append(String.format("%02X:", b));
                        }
                        if (res1.length() > 0) {
                            res1.deleteCharAt(res1.length() - 1);
                        }
                        return res1.toString();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            Log.e(TAG, "need permission :android.permission.INTERNET");
        }

        return "";
    }

    private static String getAddressMacByFile(Context context) {
        String result = "";
        String macFileAddress = SYS_CLASS_NET + getInterfaceNameByReflect() + SYS_CLASS_NET_SUFFIX;
        File fl = new File(macFileAddress);
        if (fl.exists() && fl.isFile()) {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new FileReader(fl));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result += line;
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            } finally {
                try {
                    if (null != bufferedReader) {
                        bufferedReader.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        } else {
            if (checkPermissions(context, "android.permission.INTERNET")) {
                try {
                    NetworkInterface nif = NetworkInterface.getByName(getInterfaceNameByReflect());
                    if (null != nif) {

                        byte[] macBytes = nif.getHardwareAddress();
                        if (macBytes == null) {
                            return "";
                        }
                        StringBuilder sb = new StringBuilder();
                        for (byte b : macBytes) {
                            sb.append(String.format("%02X:", b));
                        }
                        if (sb.length() > 0) {
                            sb.deleteCharAt(sb.length() - 1);
                        }
                        result = sb.toString();

                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            } else {
                Log.e(TAG, "need permission :android.permission.INTERNET");
            }
        }
        return result;
    }

    private static String getInterfaceNameByReflect() {
        String interFaceName = "";
        try {
            Class systemPropertiesClazz = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClazz.getDeclaredMethod("get", String.class, String.class);
            interFaceName = (String) getMethod.invoke(systemPropertiesClazz.newInstance(), "wifi.interface", "wlan0");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return interFaceName;
    }

    private static boolean checkPermissions(Context context, String permission) {
        try {
            PackageManager localPackageManager = context.getPackageManager();
            return localPackageManager.checkPermission(permission,
                    context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }


    public static String getMacAddressAfterSDK23() {
        String macSerial = null;
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return "00:00:00:00:00:00";
        }
        return macSerial;
    }
}
