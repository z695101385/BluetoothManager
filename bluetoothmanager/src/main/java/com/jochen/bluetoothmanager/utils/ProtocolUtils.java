package com.jochen.bluetoothmanager.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件名：ProtocolUtils
 * 描述：数据操作类
 * 创建人：jochen.zhang
 * 创建时间：2019/7/30
 */
public class ProtocolUtils {
    /**
     * Bytes数组转化为ASCII码String
     *
     * @param bytes byte[]
     * @return String
     */
    public static String bytesToAscii(byte[] bytes) {
        return bytesToAscii(bytes, 0, bytes.length);
    }

    /**
     * Bytes数组转化为ASCII码String
     *
     * @param bytes byte[]
     * @param offset 起始索引
     * @param dateLen 长度
     * @return String
     */
    public static String bytesToAscii(byte[] bytes, int offset, int dateLen) {
        if ((bytes == null) || (bytes.length == 0) || (offset < 0) || (dateLen <= 0)) {
            return null;
        }
        if ((offset >= bytes.length) || (bytes.length - offset < dateLen)) {
            return null;
        }

        String asciiStr = null;
        byte[] data = new byte[dateLen];
        System.arraycopy(bytes, offset, data, 0, dateLen);
        try {
            asciiStr = new String(data, "ISO8859-1");
        } catch (UnsupportedEncodingException e) {
        }
        return asciiStr;
    }

    /**
     * 将int类型转成协议用的1 oct字符串
     *
     * @param b int值
     * @return 字符串
     */
    public static String numToHex8(int b) {
        String result = String.format("%02x", b);
        if (result.length() > 2) {
            return result.substring(result.length() - 2);
        } else {
            return result;
        }
    }

    /**
     * 将int类型转成协议用的2 oct字符串
     *
     * @param s int值
     * @return 字符串
     */
    public static String numToHex16(int s) {
        String result = String.format("%04x", s);
        if (result.length() > 4) {
            return result.substring(result.length() - 4);
        } else {
            return result;
        }
    }

    /**
     * 将long类型转成协议用的4 oct字符串
     *
     * @param i 值
     * @return 字符串
     */
    public static String numToHex32(long i) {
        String result = String.format("%08x", i);
        if (result.length() > 8) {
            return result.substring(result.length() - 8);
        } else {
            return result;
        }
    }

    /**
     * 浮点转换为字节
     *
     * @param f
     * @return
     */
    public static byte[] float2byte(float f) {

        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(f);

        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }

        return b;
    }

    /**
     * 字节（4个字节）转换为浮点 ，保留1位小数，大端
     *
     * @param b1
     * @param b2
     * @param b3
     * @param b4
     * @return
     */
    public static float byte2float(byte b1, byte b2, byte b3, byte b4) {
        int l;
        l = b4;
        l &= 0xff;
        l |= ((long) b3 << 8);
        l &= 0xffff;
        l |= ((long) b2 << 16);
        l &= 0xffffff;
        l |= ((long) b1 << 24);
        return Math.round(Float.intBitsToFloat(l) * 10) / 10f;
    }

    /**
     * 合并4个字节为int类型
     *
     * @param high_h 最高位byte
     * @param high_l 第二位byte
     * @param low_h  第三位byte
     * @param low_l  最低位byte
     * @return 合并的int
     */
    public static int getIntFromBytes(byte high_h, byte high_l, byte low_h, byte low_l) {
        return (high_h & 0xff) << 24 | (high_l & 0xff) << 16 | (low_h & 0xff) << 8 | low_l & 0xff;
    }

    /**
     * 合并2个byte为short类型
     *
     * @param high 高位byte
     * @param low  低位byte
     * @return short结果
     */
    public static short getShortFromBytes(byte high, byte low) {
        return (short) ((high & 0xff) << 8 | (low & 0xff));
    }

    /**
     * 合并2个byte为uint类型
     *
     * @param high 高位byte
     * @param low  低位byte
     * @return int结果
     */
    public static int getUIntFromBytes(byte high, byte low) {
        return (high & 0xff) << 8 | (low & 0xff);
    }

    /**
     * 合并2个byte为int类型
     *
     * @param high 高位byte(第一位为符号)
     * @param low  低位byte
     * @return int结果
     */
    public static int getIntFromBytes(byte high, byte low) {
        //return ((high & 0x80) > 0 ? -1 : 1) * ((high & 0x7f) << 8 | (low & 0xff));
        return getShortFromBytes(high, low);
    }

    public static byte[] getBytesFromInt(int data){
        byte[] result = new byte[4];
        result[3] = (byte) (data & 0x000000ff);
        result[2] = (byte) ((data & 0x0000ff00) >> 8);
        result[1] = (byte) ((data & 0x00ff0000) >> 16);
        result[0] = (byte) ((data & 0xff000000) >> 24);

        return result;
    }

    /**
     * byte数组转成小写字符串
     *
     * @param bytes byte数组
     * @return 转换后的字符串
     */
    public static String bytesToHexStr(byte[] bytes) {
        return bytesToHexStr(bytes, false);
    }

    /**
     * byte数组转成字符串
     *
     * @param bytes     数组
     * @param isCaptial 使用大写还是小写表示
     * @return 转换后的字符串
     */
    public static String bytesToHexStr(byte[] bytes, boolean isCaptial) {
        if (null == bytes || bytes.length <= 0) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (isCaptial) {
                s.append(String.format("%02X", bytes[i]));
            } else {
                s.append(String.format("%02x", bytes[i]));
            }
        }
        return s.toString();
    }

    public static String bytesToHexStrWithSpace(byte[] bytes) {
        if (null == bytes || bytes.length <= 0) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            s.append(String.format("%02X ", bytes[i]));
        }
        return s.toString();
    }

    /**
     * 字符串转byte数组，输入前需要确认是16进制形式
     *
     * @param hex 16进制字符串
     * @return byte数组
     */
    public static byte[] hexStrToBytes(String hex) {
        if (null == hex || hex.equals("")) {
            return null;
        }
        int strLength = hex.length();
        int length = strLength / 2;
        char[] hexChars;
        if (length * 2 < strLength) { // strLength is odd, add '0'
            length += 1;
            hexChars = ("0" + hex).toCharArray();
        } else {
            hexChars = hex.toCharArray();
        }
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            bytes[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return bytes;
    }

    public static byte charToByte(char c) {
        byte result = (byte) "0123456789abcdef".indexOf(c);
        if (result == -1) {
            return (byte) "0123456789ABCDEF".indexOf(c);
        } else {
            return result;
        }
    }

    /**
     * 将b拼接在a后方
     * @param a byte[]
     * @param b byte[]
     * @return 拼接后的byte[]
     */
    public static byte[] appen(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /**
     * 截取byte数组   不改变原数组
     * @param b 原数组
     * @param off 偏差值（索引）
     * @param length 长度
     * @return 截取后的数组
     */
    public static byte[] subByte(byte[] b, int off, int length){
        byte[] b1 = new byte[length];
        System.arraycopy(b, off, b1, 0, length);
        return b1;
    }

    public static String bytesToMac(byte[] bytes) {
        if (bytes.length == 6) {
            StringBuilder mac_builder = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                mac_builder.append(String.format("%02x", bytes[i]));
                mac_builder.append(":");
            }
            mac_builder.append(String.format("%02x", bytes[5]));
            return mac_builder.toString();
        } else {
            return "";
        }
    }
}
