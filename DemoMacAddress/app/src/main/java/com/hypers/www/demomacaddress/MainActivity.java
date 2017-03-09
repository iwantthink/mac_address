package com.hypers.www.demomacaddress;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = MainActivity.this;
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "mac address :" + getMacAddress(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private String getInterfaceNameByReflect() {
        String interFaceName = "";
        try {
            Class systemPropertiesClazz = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClazz.getDeclaredMethod("get", String.class, String.class);
            interFaceName = (String) getMethod.invoke(systemPropertiesClazz.newInstance(), "wifi.interface", "wlan0");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return interFaceName;
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
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }


    private static final String MARSHMALLOW_DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
    private static final String SYS_CLASS_NET = "/sys/class/net/";
    private static final String SYS_CLASS_NET_SUFFIX = "/address";

    public String getMacAddress() {
        WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        if (wifiInfo.getMacAddress().equals(MARSHMALLOW_DEFAULT_MAC_ADDRESS)) {
            String result = getAdressMacByInterface();
            if (!TextUtils.isEmpty(result)) {
                return result;
            }
            result = getAddressMacByFile();
            if (!TextUtils.isEmpty(result)) {
                return result;
            }
            return MARSHMALLOW_DEFAULT_MAC_ADDRESS;
        } else {
            return wifiInfo.getMacAddress();
        }
    }

    private String getAdressMacByInterface() {
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
            e.printStackTrace();
        }
        return "";
    }

    private String getAddressMacByFile() {
        String result = "";
        String macFileAddress = SYS_CLASS_NET + getInterfaceNameByReflect() + SYS_CLASS_NET_SUFFIX;
        File fl = new File(macFileAddress);
        if (fl.exists() && fl.isFile()) {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new FileReader(fl));
                while (bufferedReader.readLine() != null) {
                    result += bufferedReader.readLine();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (null != bufferedReader) {
                        bufferedReader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

}
