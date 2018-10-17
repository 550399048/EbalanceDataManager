package com.eui.ebalance;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.companion.BluetoothLeDeviceFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.Tag;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chipsea.healthscale.CsAlgoBuilderEx;
import com.chipsea.healthscale.ScaleActivateStatusEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import ebalance.eui.com.ebalancedatamanager.R;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BleDataManager manager;
    private TextView textView;
    private List<BluetoothDevice> list = new ArrayList<>();
    private Button device1;
    private Button device2;
    private Button scan;
    private String TAG ="TestBLEActivity";
    private CsAlgoBuilderEx algoBuilderEx;
    private ListView listView;
    private ArrayAdapter<BluetoothDevice> adapter;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要向用户解释为什么需要申请该权限

                    //textView.setText("自Android 6.0开始需要打开位置权限才可以搜索到Ble设备");
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);

            }
        }

        manager = new BleDataManager(this);
        sharedPreferences = manager.getSharedPreferences();
        editor = sharedPreferences.edit();


        algoBuilderEx= manager.getAlgoBuilderEx();

        listView = (ListView)findViewById(R.id.listView);
        textView =(TextView)findViewById(R.id.show_data);
        device1 = (Button) findViewById(R.id.device1);
        device2 = (Button)findViewById(R.id.device2);
        scan = (Button)findViewById(R.id.startSacn);
        textView.setOnClickListener(this);
        device1.setOnClickListener(this);
        device1.setOnClickListener(this);
        scan.setOnClickListener(this);
        listView.setOnItemClickListener(this);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleDataManager.ACTION_SCAN__DISCOVER_DEVICE);
        intentFilter.addAction(BleDataManager.ACTION_BLE_CONNECT_STATE);
        intentFilter.addAction(BleDataManager.ACTION_BLE_MANUFACTIRE_DATA);// 广播包
        intentFilter.addAction(BleDataManager.ACTION_BLE_GET_DATA);

        registerReceiver(broadcastReceiver,intentFilter);
        IntentFilter intentFilter1 = new IntentFilter();
        intentFilter1.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter1.setPriority(Integer.MAX_VALUE);
        intentFilter1.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver,intentFilter1);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (manager != null) {
            if (!manager.isBluetoothEnable()) {
                manager.enableBluetooth(true);
            }
        }

    }

    private BroadcastReceiver broadcastReceiver=   new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();
          Log.d("TestBLEActivity","action =="+action);
          onHandleMessage(context,intent);
      }
    };

    public void onHandleMessage(Context context,Intent intent) {
        String action = intent.getAction();
        if (action!= null) {
            if (action.equals(BleDataManager.ACTION_BLE_CONNECT_STATE)) {
                int state = intent.getIntExtra(BleDataManager.ACTION_BLE_CONNECT_STATE, BluetoothProfile.STATE_DISCONNECTED);
                Log.d("TestBLEActivity","接收到的广播 连接的状态 connectState== "+state);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    textView.setText("已经连接");
                }
            } else if (action.equals(BleDataManager.ACTION_BLE_MANUFACTIRE_DATA)) {
                String getManufaction = intent.getStringExtra(BleDataManager.ACTION_BLE_MANUFACTIRE_DATA);
                int weight = intent.getIntExtra(BleDataManager.TYPE_DATA_MFC_WEIGHT,0);
                int resist = intent.getIntExtra(BleDataManager.TYPE_DATA_MFC_RESIST,0);
                DecimalFormat decimalFormat= new DecimalFormat("0.0");
                Log.d(TAG,"weight ==="+decimalFormat.format(weight)+",,resist ===="+decimalFormat.format(resist));
                algoBuilderEx.setUserInfo(155.0f,(byte)0,26, Float.parseFloat(decimalFormat.format(weight)),Float.parseFloat(decimalFormat.format(resist/10)));
                if (getManufaction != null) {
                    StringBuilder sb=new StringBuilder();
                    sb.append("细胞外液EXF:" + algoBuilderEx.getEXF() + "\r\n");
                    sb.append("细胞内液Inf:" + algoBuilderEx.getInF() + "\r\n");
                    sb.append("总水重TF:" + algoBuilderEx.getTF() + "\r\n");
                    sb.append("含水百分比TFR:" + algoBuilderEx.getTFR() + "\r\n");
                    sb.append("去脂体重LBM:" + algoBuilderEx.getLBM() + "\r\n");
                    sb.append("肌肉重(含水)SLM:" + algoBuilderEx.getSLM() + "\r\n");
                    sb.append("蛋白质PM:" + algoBuilderEx.getPM() + "\r\n");
                    sb.append("脂肪重FM:" + algoBuilderEx.getFM() + "\r\n");
                    sb.append("脂肪百份比BFR:" + algoBuilderEx.getBFR() + "\r\n");
                    sb.append("水肿测试EE:" + algoBuilderEx.getEE() + "\r\n");
                    sb.append("肥胖度OD:" + algoBuilderEx.getOD() + "\r\n");
                    sb.append("肌肉控制MC:" + algoBuilderEx.getMC() + "\r\n");
                    sb.append("体重控制WC:" + algoBuilderEx.getWC() + "\r\n");
                    sb.append("基础代谢BMR:" + algoBuilderEx.getBMR() + "\r\n");
                    sb.append("骨(无机盐)MSW:" + algoBuilderEx.getMSW() + "\r\n");
                    sb.append("内脏脂肪等级VFR:" + algoBuilderEx.getVFR() + "\r\n");
                    sb.append("身体年龄BodyAge:" + algoBuilderEx.getBodyAge() + "\r\n");
                    sb.append("评分:" + algoBuilderEx.getScore() + "\r\n");

                    textView.setText(sb.toString());
                    //textView.setText(getManufaction);
                    Log.d("TestBLEActivity","接收到的广播 广播数据包 == "+getManufaction);
                } else {
                    Log.d("TestBLEActivity","接收到的广播 广播数据包 是空的);");
                }
            } else if (action.equals(BleDataManager.ACTION_BLE_GET_DATA)) {
                String tempStr = intent.getStringExtra(BleDataManager.TYPE_DATA_TEMP_WEIGHT);
                String resultStr = intent.getStringExtra(BleDataManager.TYPE_DATA_FAT_RATIO);
                if (tempStr != null) {
                    Log.d("TestBLEActivity","接收到的广播 临时数据 TYPE_DATA_TEMP_WEIGHT=="+tempStr);
                }
                if (resultStr != null) {
                    Log.d("TestBLEActivity","接收到的广播 返回的最终锁定数据  TYPE_DATA_FAT_RATIO=="+resultStr);
                }

                if (tempStr !=null && resultStr != null) {
                    textView.setText("tempStr =" + tempStr + "\n"+"resultStr=="+resultStr);
                }

            } else if (action.equals(BleDataManager.ACTION_SCAN__DISCOVER_DEVICE)) {
                if (intent.getBooleanExtra(BleDataManager.TYPE_STATE_SCAN,false)) {
                    BluetoothDevice btDevice = intent.getParcelableExtra(BleDataManager.TYPE_DISCOVER_DEVICE);
                    if ( btDevice != null) {
                        Log.d("TestBLEActivity","接收到的广播 扫描到的蓝牙设备=="+btDevice.getName()+"mac"+btDevice.getAddress());

                        if (!list.contains(btDevice)) {
                            list.add(btDevice);
                            updateDevice(list);
                        }
                    }
                }
            } else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,-1);
                Log.d(TAG,"请求配对 type ====="+type);
                if (btDevice != null) {
                    try {
                        ClsUtils.setPin(btDevice.getClass(), btDevice, "0000"); // 手机和蓝牙采集器配对
                        ClsUtils.createBond(btDevice.getClass(), btDevice);
                        ClsUtils.cancelPairingUserInput(btDevice.getClass(), btDevice);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,-1);
                if (btDevice != null) {
                    Log.d(TAG,"bond state == btDevice =="+btDevice.getAddress()+",bondState ="+bondState);
                }
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    if (btDevice != null) {
                        Log.d(TAG,"绑定成功  bond state == btDevice =="+btDevice.getAddress()+",bondState ="+bondState);
                    }
                    int type = btDevice.getType() & BluetoothDevice.DEVICE_TYPE_LE;
                    if (type == BluetoothDevice.DEVICE_TYPE_LE) {
                        //manager.connect(btDevice.getAddress());
                    }
                }
            }
        }

    }



    public void updateDevice(List<BluetoothDevice> list) {
        if (list.size() >0){
            if (adapter == null) {
                adapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, list);
                listView.setAdapter(adapter);
            }else {
                adapter.notifyDataSetChanged();
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    public void onClick(View v) {
        String mac = null;
        switch (v.getId()) {
            case R.id.device1:
                 mac = (String) device1.getText();
                break;
            case R.id.device2:
                mac = (String) device2.getText();
                break;
            case R.id.show_data:
                setUserInfo();
                break;
            case R.id.startSacn:
                if(manager.isBluetoothEnable()) {
                    if (manager.isLocationEnable(this)) {
                        manager.scanLeDevice(true, 60 * 1000);
                    }
                }
                break;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO request success
                    Log.d("TestBLEActivity","wcy request success");
                }
                break;
        }
    }

    //设置用户信息

    public void setUserInfo(){
        manager.setUserInfoForBlockData(0x00, 0x1A, 0x9E, new BleDataManager.ICallBack() {
            @Override
            public void onSuccess() {
                Log.d("TestBLEActivity","设置用户信息成功");
            }

            @Override
            public void onFailed() {

            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
       BluetoothDevice btDevice = (BluetoothDevice) parent.getItemAtPosition(position);
       if (btDevice != null) {
           //manager.connect(btDevice.getAddress());
           //ClsUtils.pair(btDevice.getAddress(), "0000");
           if (!sharedPreferences.getBoolean(btDevice.getAddress(),false)) {
               manager.buildValue(btDevice.getAddress());
           }

       }
    }
}
