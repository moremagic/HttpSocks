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

/**
 *
 * @author mitsu
 */
public class Socks2StringClient extends ProxyService {

    @Override
    public Thread createSendThread(Socket input_socket, Socket output_socket) throws IOException {
        return socks2str(input_socket, output_socket);
    }

    private static Thread socks2str(final Socket input_socket, final Socket output_socket) throws IOException {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try (InputStream in = new BufferedInputStream(input_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(output_socket.getOutputStream());) {
                    int cnt = -1;
                    byte[] buffer = new byte[1024];
                    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                        String sendData = Socks2StringClient.byte2String(buffer, 0, cnt);
                        System.err.println("[" + getClass().getName() + ":socks2str]" + output_socket.getInetAddress() + ":" + output_socket.getPort() + "[send] >> " + sendData + ";");

                        out.write(sendData.getBytes());
                        out.flush();
                    }
                } catch (IOException e) {
                    //nop
                    //e.printStackTrace();
                }
            }
        };
        return new Thread(r);
    }

    public static String byte2String(byte[] data, int pos, int cnt) throws IOException {
        StringBuffer contents = new StringBuffer();
        for (int i = pos; i < cnt-pos; i++) {
            contents.append(String.format("0x%02x", (data[i] & 0xff)));
        }

        return contents.toString();
    }
}
