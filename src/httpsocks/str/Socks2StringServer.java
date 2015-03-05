/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpsocks.str;

import httpsocks.ProxyService;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mitsu
 */
public class Socks2StringServer extends ProxyService {

    @Override
    public Thread createSendThread(Socket input_socket, Socket output_socket) throws IOException {
        return str2socks(input_socket, output_socket);
    }

    private static Thread str2socks(final Socket input_socket, final Socket output_socket) throws IOException {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try (InputStream in = new BufferedInputStream(input_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(output_socket.getOutputStream());) {

                    int cnt = -1;
                    byte[] buffer = new byte[1024];
                    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                        //System.out.println(input_socket.getInetAddress() + ":" + input_socket.getPort() + "[" + new String(buffer, 0, cnt) + "]");

                        String data = new String(buffer, 0, cnt);
                        System.out.println("[" + getClass().getName() + ":str2socks]" + output_socket.getInetAddress() + ":" + output_socket.getPort() + "[send] >> " + data + ";");

                        byte[] byteData = Socks2StringServer.string2byte(data);
                        out.write(byteData);
                        out.flush();
                    }
                } catch (IOException e) {
                    //nop
                }
            }
        };
        return new Thread(r);
    }

    public static byte[] string2byte(String respons) {
        byte[] ret = new byte[respons.length() / 4];
        for (int i = 0; i < respons.length() ; i += 4) {
            ret[i / 4] = (byte) (Integer.decode(respons.substring(i, i + 4)) & 0xff);
        }
        return ret;
    }

    /**
     * debug & test
     * @param argv 
     */
    public static void main(String[] argv) {
        String data = "0x040x010x000x500xd80x3a0xdd0x0e0x00";
        
        byte[] byteData = string2byte(data);
        
        System.out.println(data);
        debugLog(byteData, byteData.length);
        
        try {
            System.out.println( data.equals( Socks2StringClient.byte2String(byteData, 0, byteData.length) )?"正常終了":"失敗" ) ;
        } catch (IOException ex) {
            Logger.getLogger(Socks2StringServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
