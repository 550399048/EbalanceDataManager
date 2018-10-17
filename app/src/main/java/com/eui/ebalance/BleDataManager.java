package com.eui.ebalance;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.chipsea.healthscale.CsAlgoBuilderEx;
import com.chipsea.healthscale.ScaleActivateStatusEvent;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.DecimalNode;

import java.lang.reflect.InvocationTargetException;
import java.security.PublicKey;
import java.security.acl.LastOwnerException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by wucaiyan on 2017/11/16.
 */

public class BleDataManager implements BluetoothLeService.IDataAvailableListener, BluetoothLeService.IServicesDiscoveredListener, BluetoothLeService.IConnectedListener {
    private final static String TAG = "BleDataManager";

    protected static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final String UUID_UNLOCK_DATA_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static final String UUID_UNLOCK_DATA_NOTIFY = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static final String UUID_UNLOCK_DATA_WRITE = "0000fff2-0000-1000-8000-00805f9b34fb";
    public static final int BLE_PROTOCPL_PKLEN = 20;


    private static final int CMMD_HEADER_BYTE = 0xFA;
    //设置用户数据的请求与应答
    private static final int CMMD_USERINFO_REQUEST_RESPOND = 0x01;
    //接收临时数据CM 2byte
    private static final int TYPE_TEMP_DATA = 0x03;
    //锁定数据接收和应答CM 2byte
    private static final int BLOCK_DATA_REQUST_RESPOND = 0x04;


    public static final String ACTION_BLE_GET_DATA = "com.eui.ebalance.ACTION_BLE_GET_DATA";
    public static final String ACTION_BLE_MANUFACTIRE_DATA = "com.eui.ebalance.ACTION_BLE_MANUFACTIRE_DATA";
    public static final String ACTION_BLE_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    public static final String ACTION_BLE_CONNECT_STATE = "com.eui.ebalance.ACTION_BLE_CONNECT_STATE";
    public static final String ACTION_SCAN__DISCOVER_DEVICE = "com.eui.ebalance.ACTION_SCAN_DISCOVER_DEVICE";

    public static final String TYPE_DATA_FAT_RATIO = "com.eui.ebalance.TYPE_DATA_USERINFOS";
    public static final String TYPE_DATA_TEMP_WEIGHT = "com.eui.ebalance.TYPE_DATA_TEMP_WEIGHT";
    public static final String TYPE_DISCOVER_DEVICE = "com.eui.ebalance.TYPE_DISCOVER_DEVICE";
    public static final String TYPE_STATE_SCAN = "com.eui.ebalance.TYPE_STATE_SCAN";

    public static final String TYPE_DATA_MFC_WEIGHT = "com.eui.ebalance.TYPE_DATA_MFC_WEIGHT";
    public static final String TYPE_DATA_MFC_RESIST = "com.eui.ebalance.TYPE_DATA_MFC_RESIST";



    private final Handler mHandler;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static BluetoothGattCharacteristic gattCharacteristic_write;
    public static BluetoothGattCharacteristic gattCharacteristic_notify;

    private Context mContext;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private List<BluetoothDevice> mScanLeDeviceList = new ArrayList<>();
    private boolean isBluetoothServiceEnable;
    private boolean isScanning;
    private boolean isSuccess1 = false,isSeccess2 = false;
    private float mWeight;
    private String weightUnits;
    private int tag1;
    private int tag2;
    private int weightStr;
    private static int countSame;
    private int mResistance;
    private CsAlgoBuilderEx algoBuilderEx;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public BleDataManager(Context context) {
        mContext = context;
        isBluetoothServiceEnable = initialize();
        sharedPreferences = mContext.getSharedPreferences("SDK_AUTH_STATU",MODE_PRIVATE);
        editor = sharedPreferences.edit();

        algoBuilderEx=new CsAlgoBuilderEx(mContext);
        if (isBluetoothServiceEnable) {
            mBluetoothLeService = new BluetoothLeService(context, mBluetoothAdapter, mBluetoothManager);
        }
        mHandler = new Handler();
        mBluetoothLeService.setDataAvailableListener(this);
        mBluetoothLeService.setServicesDiscoveredListener(this);
        mBluetoothLeService.setStateConnected(this);
    }


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);

            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public boolean enableBluetooth(boolean enable) {
        if (enable) {
            if (!mBluetoothAdapter.isEnabled()) {
                return mBluetoothAdapter.enable();
            }
            return true;
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                return mBluetoothAdapter.disable();
            }
            return false;
        }
    }

    public boolean isBluetoothEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    public  boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (networkProvider || gpsProvider) return true;
        return false;
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG,"onCharacteristicWrite ");
        if (characteristic.getUuid().equals(UUID_UNLOCK_DATA_WRITE)) {
            if (characteristic.getValue() != null && characteristic.getValue().length > 3) {
                byte[] tempRespond = characteristic.getValue();
                Log.d(TAG,"onCharacteristicWrite ==="+BleUtils.getHexString(tempRespond));
                if (tempRespond[1] == 0x01 && tempRespond[2] == 0x01) {
                    //successs
                } else {
                    //failed
                }
            }
        }

    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG,"onCharacteristicChanged ");
        if (characteristic.getUuid().toString().equals(BleDataManager.UUID_UNLOCK_DATA_NOTIFY)) {
            doneCharacteristicChangedData(gatt,characteristic);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt bluetoothGatt) {
        Log.d(TAG, "onServicesDiscovered 获取到属性service");
        displayGattServices(mBluetoothLeService.getSupportedGattServices());
    }

    @Override
    public void onConnect(BluetoothGatt bluetoothGatt, int status, int newStatus) {
        Log.d(TAG, "onConnect");
        Intent intent = new Intent(ACTION_BLE_CONNECT_STATE);
        if (newStatus == BluetoothProfile.STATE_CONNECTED) {
            mHandler.removeCallbacks(mConnTimeOutRunnable);
            intent.putExtra(ACTION_BLE_CONNECT_STATE, newStatus);
        } else if (newStatus == BluetoothProfile.STATE_DISCONNECTED) {
            intent.putExtra(ACTION_BLE_CONNECT_STATE, newStatus);
        }
        sendBroadcastForResult(intent);
    }


    /**
     * get the supported characteristics , maybe need to change
     *
     * @param gattServices gattServices
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG, "displayGattServices getService ====" + gattService.getUuid() + ",," + gattService.getType());
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.d(TAG, "displayGattServices gattCharacteristic uuid ==" + gattCharacteristic.getUuid() + ",," + gattCharacteristic.getProperties());
                if (gattCharacteristic.getValue() != null) {
                    Log.d(TAG, "displayGattServices value==" + gattCharacteristic.getValue());
                }
                if (gattCharacteristic.getUuid().toString().equals(BleDataManager.UUID_UNLOCK_DATA_NOTIFY)) {
                    Log.d(TAG,"设置notyfy");
                    BleDataManager.gattCharacteristic_notify = gattCharacteristic;
                    mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                }
                if (gattCharacteristic.getUuid().toString().equals(BleDataManager.UUID_UNLOCK_DATA_WRITE)) {
                    BleDataManager.gattCharacteristic_write = gattCharacteristic;
                }
            }
        }
    }


    /**
     * 接收临时数据
     */
    public void  doneCharacteristicChangedData(BluetoothGatt gatt, BluetoothGattCharacteristic character) {
        Log.d(TAG,"doneCharacteristicChangedData  接收数据");
        int optionCMMD = 0x00;
        String dataOutHead = null;
        Intent intent;
            if (character.getValue() == null || character.getValue().length < 20) {
                Log.d(TAG, "character.getUuid().toString() not respond data");
            } else {
                byte[] dataByteArray = character.getValue();
                String dataString = BleUtils.getHexString(dataByteArray);
                optionCMMD = dataByteArray[1];
                Log.d(TAG, "doneCharacteristicChangedData=" + dataString+",optionCMMD=="+optionCMMD);
                switch (optionCMMD) {
                    case BleUtils.CMMD_GET_TEMP_DATA:
                        //透传协议 3-4 weight
                        String weight = dataString.substring(2, 4);
                        int msgAttrValue = BleUtils.getMsgValueFromBtArray(dataByteArray,4);
                        Log.d(TAG,"获取到的临时数据 ==="+dataString+"msgAttrValue = "+msgAttrValue);
                        intent = new Intent(ACTION_BLE_GET_DATA);
                        intent.putExtra(TYPE_DATA_TEMP_WEIGHT, dataOutHead);
                        sendBroadcastForResult(intent);
                        break;
                    case BleUtils.CMMD_SET_USER_RESPOND:
                         int isSuccessTag = BleUtils.getMsgValueFromBtArray(dataByteArray,2);
                         if (isSuccessTag == 0x01) {
                             Log.d(TAG,"设置用户信息返回的值 isSuccessTag = "+isSuccessTag);
                         } else {

                             Log.d(TAG,"设置用户信息返回的值 isFailedTag =请重新测量 "+isSuccessTag);
                         }

                        break;
                    case BleUtils.CMMD_GET_RESULT_DATA:
                        int dataPos = BleUtils.getMsgValueFromBtArray(dataByteArray,2);
                        if (dataPos == 0x01) {
                            tag1 ++;
                            Log.d(TAG,"返回的用户数据是第一帧===="+dataString.substring(6,10));
                            Log.d(TAG,"返回的用户数据是第一帧 "+dataString);
                            if (BleUtils.isValidBleByteArray(dataByteArray,"用户数据是第一帧")){
                                if (analyzeMeasurResult(dataByteArray,dataString,0) != null) {
                                    isSuccess1 = true;
                                }
                            }
                        } else if (dataPos == 0x02) {
                            tag2 ++;
                            Log.d(TAG," 返回的用户数据是二帧====="+ dataString);
                            if (analyzeMeasurResult(dataByteArray,dataString,1) != null) {
                                isSeccess2 = true;
                            }
                        } else {
                            onReceivedLockDataResponse((byte) 0x01);
                        }
                        if (isSuccess1 && isSeccess2) {
                            onReceivedLockDataResponse((byte) 0x01);
                            intent = new Intent(ACTION_BLE_GET_DATA);
                            intent.putExtra(TYPE_DATA_FAT_RATIO, dataOutHead);
                            sendBroadcastForResult(intent);
                            isSuccess1 = false;
                            isSeccess2 = false;
                        }

                        break;
                    default:
                        Log.d(TAG, "not same data");
                }
            }

    }
    public String analyzeMeasurResult(byte[] btArray,String str,int num) {
        String[] description = {"BWater", "BMuscle", "WProtein", "BFat", "BasalMetabolism",
                "WSkeleton", "LVisceralFat", "WLean", "BodyAge", "BMI", "WFat"};
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        StringBuilder stringBuilder = new StringBuilder();
        String tempStr = null;
        float tempFloat = 0;
        if (num == 0) {
            tempStr = str.substring(6, 10);
            tempFloat = Integer.valueOf(tempStr, 16);
            if (tempFloat > 0) {
                //消息属性值
                int attrValue = Integer.valueOf(str.substring(10, 12), 16);
                String attBinary = BleUtils.decimalToBinaryStr(attrValue);
                switch (attBinary.substring(5, 7)) {
                    //1
                    case "00":
                        mWeight = tempFloat / 10;
                        break;
                    //0
                    case "01":
                        mWeight = tempFloat;
                        break;
                    //2
                    case "10":
                        mWeight = tempFloat / 100;
                        break;

                }

                switch (attBinary.substring(3, 5)) {
                    case "00":
                        weightUnits = "kg";
                        break;
                    case "01":
                        weightUnits = "斤";
                        break;
                    case "10":
                        weightUnits = "LB";
                        break;
                    case "11":
                        weightUnits = "ST:LB";
                        break;
                }
            } else {
                Log.d(TAG,"analyzeMeasurResult weight == 0 !");
                onReceivedLockDataResponse((byte) 0x02);
                return null;
            }
            stringBuilder.append("体重 = " + mWeight + weightUnits + "\n");

            int len = str.length();
            int index = 12;
            while (index < len) {
                tempStr = str.substring(index, index + 4);
                tempFloat = Integer.valueOf(tempStr, 16);
                if (tempFloat > 0) {
                    stringBuilder.append(description[(index - 12) / 4] + "==" + decimalFormat.format(tempFloat / 10));
                    index = index + 4;
                } else {
                    Log.d(TAG,"analyzeMeasurResult" +description[(index - 12) / 4]+"== 0 !");
                    onReceivedLockDataResponse((byte) 0x02);
                    return null;
                }
            }
        } else {
            int len = str.length();
            int index = 6;
            while (index < len) {
                tempStr = str.substring(index, index + 4);
                tempFloat = Integer.valueOf(tempStr, 16);
                if (tempFloat > 0) {
                    stringBuilder.append(description[(index - 6) / 4] + "==" + decimalFormat.format(tempFloat / 10));
                    index = index + 4;
                } else {
                    Log.d(TAG,"analyzeMeasurResult"+description[(index - 6) / 4]+" == 0 !");
                    onReceivedLockDataResponse((byte) 0x02);
                    return null;
                }
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 发送结果广播
     */

    private void sendBroadcastForResult(Intent intent) {
        mContext.sendBroadcast(intent);
    }


    /**
     * 连接超时，回调
     */
    private Runnable mConnTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            //资源释放
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            if (mScanLeDeviceList != null) {
                mScanLeDeviceList.clear();
                mScanLeDeviceList = null;
            }
        }
    };

    /**
     * 连接 可能会超时 时间待定
     */
    public void connect(final String address) {
        mHandler.postDelayed(mConnTimeOutRunnable, 90 * 1000);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //真正开始连接设备
                Log.d(TAG,"真正开始连接 address ==" + address);
                if (isScanning) scanLeDevice(false, 1000);
                mBluetoothLeService.close();
                mBluetoothLeService.connect(address);
                Intent intent = new Intent(ACTION_BLE_CONNECT_STATE);
                intent.putExtra(ACTION_BLE_CONNECT_STATE, BluetoothProfile.STATE_CONNECTING);
                sendBroadcastForResult(intent);
            }
        }, 100);
    }

    /**
     * 开始扫描 扫描一定时间以后停止扫描 时间待定
     *
     * @param enable     scan start:true stop:false
     * @param scanPeriod scan time
     */
    public void scanLeDevice(final boolean enable, long scanPeriod) {
        if (isScanning) return;
        if (enable) {
            //Stop scanning after a predefined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopLeScan();
                    //mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                }
            }, scanPeriod);
            if (mScanLeDeviceList == null) {
                mScanLeDeviceList = new ArrayList<>();
            }
            mScanLeDeviceList.clear();
            isScanning = true;
            mBluetoothAdapter.startLeScan(mlLeScanCallback);
            Intent intent = new Intent(ACTION_SCAN__DISCOVER_DEVICE);
            intent.putExtra(TYPE_STATE_SCAN,isScanning);
            sendBroadcastForResult(intent);
        } else {
            stopLeScan();
        }
    }

    public void stopLeScan() {
        isScanning = false;
        mBluetoothAdapter.stopLeScan(mlLeScanCallback);
        Intent intent = new Intent(ACTION_SCAN__DISCOVER_DEVICE);
        intent.putExtra(TYPE_STATE_SCAN,isScanning); //如果false的时候可获取设备信息
        sendBroadcastForResult(intent);
        /*if (mScanLeDeviceList != null) {
            mScanLeDeviceList.clear();
            mScanLeDeviceList = null;
        }*/
    }

    public List<BluetoothDevice> getScanLeDevice() {
        return mScanLeDeviceList;
    }

    private BluetoothAdapter.LeScanCallback mlLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @SuppressLint("LongLogTag")
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            String scanRecordStr= null;
            if (scanRecord != null) {
                scanRecordStr= BleUtils.bytesToHexString(scanRecord);
                if (device != null && device.getName()!= null&&(device.getName().equals("SENSSUN") || scanRecordStr.substring(10,18).equals("FAFB0101"))) {
                    Log.d(TAG, "descripter 开始连接 ==" + scanRecordStr);
                    //send MANUFACTIRE_DATA
                    sendBleManufactireData(device,scanRecordStr);
                    if (!mScanLeDeviceList.contains(device)){
                        mScanLeDeviceList.add(device);
                        Intent intent = new Intent(ACTION_SCAN__DISCOVER_DEVICE);
                        intent.putExtra(TYPE_STATE_SCAN,isScanning);
                        intent.putExtra(TYPE_DISCOVER_DEVICE,device);
                        sendBroadcastForResult(intent);
                    }
                    Log.d(TAG, "device name =" + device.toString() + "," + device.getUuids() + "," + device.getName() + "," + device.getBondState() + "," + device.getClass());


                }
            }
        }
    };



    public void sendBleManufactireData(BluetoothDevice btDevice,String scanRecordStr) {
        mDeviceAddress = btDevice.getAddress();
        String bleMac = scanRecordStr.substring(38,40)+
                ":"+scanRecordStr.substring(40,42) +
                ":"+scanRecordStr.substring(42,44) +
                ":"+scanRecordStr.substring(44,46) +
                ":"+scanRecordStr.substring(46,48) +
                ":"+scanRecordStr.substring(48,50);
        Log.d(TAG,"sendBleManufactireData mDeviceAddress =="+mDeviceAddress+" ,bleMac=="+bleMac+" ,"+sharedPreferences.getBoolean(mDeviceAddress,false));

        if (mDeviceAddress != null && sharedPreferences.getBoolean(mDeviceAddress,false) && bleMac.equals(mDeviceAddress)) {
            int tempW = Integer.valueOf(scanRecordStr.substring(18, 22),16);
            int tempR = Integer.valueOf(scanRecordStr.substring(24, 28),16);
            if (weightStr != 0 && tempW == weightStr && mResistance != 0 && tempR == mResistance) {
                countSame ++;
            } else {
                weightStr = tempW;
                mResistance = tempR;
            }

            if (countSame > 3) {
                if (scanRecordStr.substring(22,24).equals("04")) {
                    tempW = tempW/10;
                } else {
                    tempW = tempW/100;
                }
                Log.d(TAG,"countSame=="+countSame+" ,tempW="+tempW+" ,tempR=="+tempR);
                Intent intent = new Intent(ACTION_BLE_MANUFACTIRE_DATA);
                intent.putExtra(ACTION_BLE_MANUFACTIRE_DATA,scanRecordStr);
                intent.putExtra(TYPE_DATA_MFC_WEIGHT, tempW);
                intent.putExtra(TYPE_DATA_MFC_RESIST,tempR);
                sendBroadcastForResult(intent);
                countSame = 0;
            }
            //connect(mDeviceAddress);
            //stopLeScan();
            Log.d(TAG,"扫描到的该设备已被绑定 mac =" + mDeviceAddress);//此处需要告诉用户进行配对，手动点击进行配对
        }
    }

    //绑定设备
    public void  bondDevice (BluetoothDevice bluetoothDevice,ICallBack iCallBack) {
        Log.d(TAG,"设备是否已经绑定 "+bluetoothDevice.getBondState());
        if (bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                if (bluetoothDevice.createBond()) {
                    iCallBack.onSuccess();
                } else {
                    iCallBack.onFailed();
                }

        }
    }

    //参数是16进制的数 0x00
    public void setUserInfoForBlockData(int sex, int age, int height, ICallBack iCallBack) {
        Log.d(TAG, "setUserInfoForBlockData ()");
        if (sex > 255 || age > 255 || height > 255) {
            Log.d(TAG, "sex age height Not a valid two hexadecimal");
            iCallBack.onFailed();
        }
        byte[] userByteArray = new byte[20];
        byte[] temp = null;
        userByteArray[0] = (byte) CMMD_HEADER_BYTE;
        userByteArray[1] = CMMD_USERINFO_REQUEST_RESPOND;
        userByteArray[2] = (byte) sex;
        userByteArray[3] = (byte) age;
        userByteArray[4] = (byte) height;
        temp = BleUtils.getUTCSeconds();
        Log.d(TAG,"时间是 temp="+temp.length);
        if (temp.length == 4) {
            for (int i = 0; i < 4; i++) {
                userByteArray[i + 5] = temp[i];
            }
        }
        userByteArray[19] = BleUtils.XORCheckSum(userByteArray);
        BleDataManager.gattCharacteristic_write.setValue(userByteArray);
        mBluetoothLeService.writeCharacteristic(BleDataManager.gattCharacteristic_write, iCallBack);
    }

    public void onReceivedLockDataResponse (byte successBit) {
        Log.d(TAG,"回复称已经收到搜定信息");
        byte[] bytes = new byte[20];
        bytes[0] = (byte) 0xFA;
        bytes[1] = 0x04;
        bytes[2] = successBit;
        bytes[19] = BleUtils.XORCheckSum(bytes);
        BleDataManager.gattCharacteristic_write.setValue(bytes);
        mBluetoothLeService.writeCharacteristic(BleDataManager.gattCharacteristic_write, new ICallBack() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"发送应答成功");
            }

            @Override
            public void onFailed() {

            }
        });
    }

    public void buildValue(final String mac){
        ConnectivityManager connectivity = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected() && info.getState() == NetworkInfo.State.CONNECTED) {
                if(!algoBuilderEx.getAuthStatus()) {
                    //激活方法，正式激活请将mac地址替换为蓝牙秤的蓝牙mac地址
                    algoBuilderEx.Authorize(mac, new ScaleActivateStatusEvent() {
                        @Override
                        public void onActivateSuccess() {
                            Log.i(TAG, "激活成功!");
                            editor.putBoolean(mac,true);
                            editor.commit();
                        }

                        @Override
                        public void onActivateFailed() {
                            //如果激活的mac地址激活次数过多，或者mac地址不是授权的蓝牙地址SDK会被冻结
                            Log.i(TAG, "激活失败,SDK被冻结!");
                            //editor.putBoolean(mac,false);
                            //editor.commit();
                        }
                        @Override
                        public void onHttpError(int i, String s) {
                            Log.i(TAG, "激活失败,需要重新激活,ErrCode:" + i);
                            editor.putBoolean(mac,false);
                            editor.commit();
                        }
                    });
                }
            } else {
                Toast.makeText(mContext,"请确保网络状态已经可访问", Toast.LENGTH_SHORT).show();
            }
        }

    }
    public CsAlgoBuilderEx getAlgoBuilderEx(){
        if(algoBuilderEx == null){
           algoBuilderEx = new CsAlgoBuilderEx(mContext);
        }
        return algoBuilderEx;
    }

    public SharedPreferences getSharedPreferences(){
        if (sharedPreferences == null) {
            sharedPreferences = mContext.getSharedPreferences("SDK_AUTH_STATU",MODE_PRIVATE);
        }
        return sharedPreferences;
    }

    public interface ICallBack {
        void onSuccess();

        void onFailed();
    }


}
