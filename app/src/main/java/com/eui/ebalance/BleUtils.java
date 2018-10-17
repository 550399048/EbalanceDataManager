package com.eui.ebalance;

import android.util.Log;

import java.util.Random;

/**
 * Created by wucaiyan on 2017/11/16.
 */

public class BleUtils {
    private final static String TAG ="BleUtils";
    private final static boolean DEBUG = false;
    //厂商的ID：占两个字节 协议ID：0x01 产品ModeID：0x0115
    public static final int COUNT_MANUFACTURERS_DATA = 11;
    public static final int CMMD_GET_RESULT_DATA = 0x04 ;
    public static final int CMMD_GET_TEMP_DATA = 0x03;
    public static final int CMMD_SET_USER_RESPOND = 0x01;
    public static final int BLE_PROTOCPL_PKLEN = 20;
    public static final int DEFAULT_BYTE = 0x00;

    /**
     * @param data byteArray
     * @return Hexadecimal string
     */
    public static String getHexString(byte[] data) {
        StringBuffer stringBuffer = null;
        if (data != null && data.length > 0) {
            stringBuffer = new StringBuffer(data.length);
            for (byte byteChar : data) {
                stringBuffer.append(String.format("%02X", byteChar));
            }
        }
        return stringBuffer.toString();
    }


    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        return result;
    }


    /**
     * 从十六进制字符串到字节数组转换
     *  hexstr 0x00
     * */
    public static byte[] hexString2Bytes(String hexstr) {
        int length=hexstr.length();
        if(length%2!=0){
            throw new RuntimeException("Hex  bit string length must be even");
        }
        byte[] b = new byte[hexstr.length() / 2];
        int j = 0;
        for (int i = 0; i < b.length; i++) {
            char c0 = hexstr.charAt(j++);
            char c1 = hexstr.charAt(j++);
            Log.d(TAG,"getchar c0=="+c0+",c1=="+c1);
            b[i] = (byte) ((parse(c0) << 4) | parse(c1));
            if (DEBUG) {
                Log.d(TAG,"b[+"+i+"]"+"parsec0="+(parse(c0)<<4)+"cparsec1="+parse(c1)+","+b[i]);
            }
        }
        return b;
    }

    private static int parse(char c) {
        if (c >= 'a')
            return (c - 'a' + 10) & 0x0f;
        if (c >= 'A')
            return (c - 'A' + 10) & 0x0f;
        return (c - '0') & 0x0f;
    }



    /**
     *  构造验证码:长度+功能码+透传数据的十六进制和的低八位
     * */
    public static byte[] constructionCheckCode (String data) {
        if (data == null || data.equals("")) {
            return hexString2Bytes(0 +"");
        }
        int len = data.length();
        int num = 8;
        if (len <8) {
            throw new RuntimeException("Character length must be greater than 8");
        }
        Log.d(TAG,"constructionCheckCode==len=="+len);
        int total = 0;
        while (num < len) {
            total += Integer.parseInt(data.substring(num,num+2), 16);
            if (DEBUG) {
                Log.d(TAG,"data.substring(num,num+2)= "+data.substring(num,num+2)+"Integer="+Integer.parseInt(data.substring(num,num+2),16));
            }
            num = num + 2;
        }
        String hex = Integer.toHexString(total);
        len = hex.length();
        // 如果不够校验位的长度，补0,这里用的是两位校验
        if (len < 1) {
            hex = "00";
        } else if (len <2){
            hex ="0"+hex;
        } else {
            hex = hex.substring(len-2);
        }
        Log.d(TAG,"constructionCheckCode result =="+hexString2Bytes(hex));
        return hexString2Bytes(hex);
    }

    public byte[] getRandomNum(int count){
        StringBuffer sb = new StringBuffer();
        String str = "0123456789";
        Random r = new Random();
        for(int i=0;i<count;i++){
            int num = r.nextInt(str.length());
            sb.append(str.charAt(num));
            str = str.replace((str.charAt(num)+""), "");
        }
        return hexString2Bytes(str);
    }
    /**
     * @param data is byte1-byte19
     * @return CheckCode:Exclusive OR of byte 1 to byte 19
     * */
    public static byte XORCheckSum (byte[] data) {
        if (data.length == 0) {
            return DEFAULT_BYTE;
        }
        byte result = data[0];
        for (int i=1;i <data.length;i++) {
            result = (byte) (result|data[i]);
        }
        return result;
    }

    /**
     * @return Four bytes of utc total seconds
     */
    public static byte[] getUTCSeconds() {
        byte[] secondsByte = null;
        long totalSeconds = System.currentTimeMillis()/1000;
        Log.d(TAG,"getUTCSeconds totalSeconds=="+totalSeconds);
        secondsByte = BleUtils.hexString2Bytes(String.valueOf(totalSeconds).substring(0,8));
        if (secondsByte.length == 0) {
            secondsByte = new byte[]{0x00, 0x00, 0x00, 0x00};
        }
        return secondsByte;
    }


    /**
     *   消息属性是重量的单位格式：
     *   bit7 bit6 bit5  bit4bit3 bit2bit1    bit0
     *                  kg 0 0   1位 0 0 unlock 0
     *                  斤 0 1   0位 0 1  lock  1
     *                  LB 1 0   2位 1 0
     *               ST:LB 1 1
     */
    public static String decimalToBinaryStr (int decinal) {
        if (decinal < 0 || decinal > 255) {
            Log.d(TAG,"decinal value error!");
            return "00000100";  //04:kg 0.0
        }
        String binaryStr = Integer.toBinaryString(decinal);
        if (binaryStr != null) {
            int len = binaryStr.length();
            if (DEBUG) Log.d(TAG, "属性值attrValuebinaryStr ==" + binaryStr);
            if (len < 8) {
                int nume = 0;
                do {
                    binaryStr = "0" + binaryStr;
                    nume = nume + 1;
                } while (nume < 8 - len);
            }
        }
        return binaryStr;
    }

     /**
     * @param msg The received byte array instruction, from scale to app
     * @param positionAttr  The location of the MsgBodyAttr in the byte array
     * @return  Msg message body attributes, weight units and decimal places, unlock/lock data
     *         default 0x00 kg, 1 decimal place, unlock
     */
    public static byte getMsgValueFromBtArray(byte[] msg,int positionAttr) {
        if (!isValidBleByteArray(msg,"getMsgBodyAttr")) {
            return DEFAULT_BYTE;
        }
        if (msg.length < positionAttr || positionAttr == -1 || positionAttr > BLE_PROTOCPL_PKLEN ) {
            Log.d(TAG,"getMsgBodyAttr msg.len <"+positionAttr);
            return DEFAULT_BYTE;
        }
        return msg[positionAttr];
    }

    /**
     * @param msg The received/send byte array instruction, scale <-> app
     * @return  true isValid
     */
    public static boolean isValidBleByteArray(byte[] msg,String method) {
        if (msg == null) {
            Log.d(TAG,"isValidBleByteArray (msg == null) method = "+method);
            return false;
        }
        int len = msg.length;
        if (len < BLE_PROTOCPL_PKLEN || len > BLE_PROTOCPL_PKLEN) {
            Log.d(TAG,"isValidBleByteArray msg.len error  =="+len+" ,method = "+method);
            return false;
        }
        return true;
    }


}
