package com.hypers.www.demomacaddress;

import android.content.Context;
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
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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
                mTvInfo.setText("mac address :" + getMacAddress() + "\n");
                Log.d("MainActivity", "mac address =" + getMacAddress());
                Log.d("MainActivity", "file = " + getAddressMacByFile());
                Log.d("MainActivity", "interface =" + getAdressMacByInterface());
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

    public String getMacAddress() {
        WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        if (wifiInfo.getMacAddress().equals(MARSHMALLOW_DEFAULT_MAC_ADDRESS)) {
            String result = getAdressMacByInterface();
            if (!TextUtils.isEmpty(result)) {
                return result.toLowerCase();
            }
            result = getAddressMacByFile();
            if (!TextUtils.isEmpty(result)) {
                return result.toLowerCase();
            }
            return MARSHMALLOW_DEFAULT_MAC_ADDRESS;
        } else {
            return wifiInfo.getMacAddress().toLowerCase();
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
            Log.e("MainActivity", e.getMessage());
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
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result += line;
                }
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage());
            } finally {
                try {
                    if (null != bufferedReader) {
                        bufferedReader.close();
                    }
                } catch (IOException e) {
                    Log.e("MainActivity", e.getMessage());
                }
            }
        } else {
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
                Log.e("MainActivity", e.getMessage());
            }
        }
        return result;
    }

    private String getInterfaceNameByReflect() {
        String interFaceName = "";
        try {
            Class systemPropertiesClazz = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClazz.getDeclaredMethod("get", String.class, String.class);
            interFaceName = (String) getMethod.invoke(systemPropertiesClazz.newInstance(), "wifi.interface", "wlan0");
        } catch (Exception e) {
            Log.e("MainActivity", e.getMessage());
        }
        return interFaceName;
    }

}
