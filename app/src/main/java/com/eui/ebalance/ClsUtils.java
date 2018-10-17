package com.eui.ebalance;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by wucaiyan on 2017/11/20.
 */

class ClsUtils {
    /**
     * 与设备配对
     */
    public static boolean createBond(Class btClass, BluetoothDevice bluetoothDevice) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(bluetoothDevice);
        return returnValue;
    }

    /**
     * removeBond
     */
    public static boolean removeBond(Class<?> btClass, BluetoothDevice bluetoothDevice) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(bluetoothDevice);
        return returnValue;
    }

    public static boolean setPin(Class<? extends BluetoothDevice> btClass, BluetoothDevice bluetoothDevice, String str) {
        Method removeBondMethod = null;
        try {
            removeBondMethod = btClass.getDeclaredMethod("setPin", new Class[]{byte[].class});
            boolean returnValue = (boolean) removeBondMethod.invoke(bluetoothDevice, new Object[]{str.getBytes()});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 取消用户输入
     */
    static public boolean cancelPairingUserInput(Class<?> btClass,
                                                 BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
//        cancelBondProcess(btClass, device);
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    /**
     * 取消配对
     */
    static public boolean cancelBondProcess(Class<?> btClass, BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    /**
     * 确认配对
     */
    static public void setPairingConfirmation(Class<?> btClass, BluetoothDevice device, boolean isConfirm) throws Exception {
        Method setPairingConfirmation = btClass.getDeclaredMethod("setPairingConfirmation", boolean.class);
        setPairingConfirmation.invoke(device, isConfirm);
    }


    static public void printAllInform(Class clsShow) {
        try {
            // 取得所有方法
            Method[] hideMethod = clsShow.getMethods();
            int i = 0;
            for (; i < hideMethod.length; i++) {
                Log.e("method name", hideMethod[i].getName() + ";and the i is:"
                        + i);
            }
            // 取得所有常量
            Field[] allFields = clsShow.getFields();
            for (i = 0; i < allFields.length; i++) {
                Log.e("Field name", allFields[i].getName());
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    static public boolean pair(String strAddr, String strPsw) {
        boolean result = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();

        bluetoothAdapter.cancelDiscovery();

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

        if (!BluetoothAdapter.checkBluetoothAddress(strAddr)) { // 检查蓝牙地址是否有效

            Log.d("mylog", "devAdd un effient!");
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(strAddr);

        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            try {
                Log.d("mylog", "NOT BOND_BONDED");
                ClsUtils.setPin(device.getClass(), device, strPsw); // 手机和蓝牙采集器配对
                ClsUtils.createBond(device.getClass(), device);
// remoteDevice = device; // 配对完毕就把这个设备对象传给全局的remoteDevice
                result = true;
            }
            catch (Exception e) {
// TODO Auto-generated catch block

                Log.d("mylog", "setPiN failed!");
                e.printStackTrace();
            } //

        }
        else {
            Log.d("mylog", "HAS BOND_BONDED");
            try {
                ClsUtils.createBond(device.getClass(), device);
                ClsUtils.setPin(device.getClass(), device, strPsw); // 手机和蓝牙采集器配对
                ClsUtils.createBond(device.getClass(), device);
// remoteDevice = device; // 如果绑定成功，就直接把这个设备对象传给全局的remoteDevice
                result = true;
            }
            catch (Exception e) {
// TODO Auto-generated catch block
                Log.d("mylog", "setPiN failed!");
                e.printStackTrace();
            }
        }
        return result;
    }


}
