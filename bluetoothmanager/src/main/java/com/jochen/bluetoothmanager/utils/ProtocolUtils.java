package com.jochen.bluetoothmanager.utils;

import java.io.UnsupportedEncodingException;

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
}
