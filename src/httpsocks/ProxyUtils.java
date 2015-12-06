/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpsocks;

import java.util.List;

/**
 *
 * @author mitsu
 */
public class ProxyUtils {
    public static final byte[] _HttpSeparator = new byte[]{(byte) (0x0d & 0xff), (byte) (0x0a & 0xff), (byte) (0x0d & 0xff), (byte) (0x0a & 0xff)};

    public static byte[] byteList2ByteArrays(List<Byte> byteList) {
        byte[] ret = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            ret[i] = byteList.get(i);
        }
        return ret;
    }

    public static boolean containData(List<Byte> data, byte[] find) {
        int cnt = 0;
        for (int i = 0; i < data.size(); i++) {
            if (find[cnt] == (byte) data.get(i)) {
                if (++cnt == find.length - 1) {
                    return true;
                }
            } else {
                cnt = 0;
            }
        }
        return false;
    }

    
    public static boolean isResponseEnd(List<Byte> data) {
        String response = new String( byteList2ByteArrays(data) );
        if( response.contains("Transfer-Encoding: chunked") ){
            return response.endsWith("0\r\n\r\n");
        }else{
            return response.endsWith("\n");
        }
    }

    
    public static void debugLog(byte[] data, int cnt) {
        //DEBUG LOG
        /*
        for (int i = 0; i < cnt; i++) {
            System.out.print("0x" + Integer.toHexString(data[i] & 0xff));
        }
         */
        System.out.print(new String(data, 0, cnt));
        System.out.println();
    }
}