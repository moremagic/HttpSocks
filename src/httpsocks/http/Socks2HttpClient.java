/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpsocks.http;

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
public class Socks2HttpClient extends ProxyService {

    @Override
    public Thread createSendThread(Socket input_socket, Socket output_socket) throws IOException {
        return socks2http(input_socket, output_socket);
    }

    private static Thread socks2http(final Socket input_socket, final Socket output_socket) throws IOException {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try (InputStream in = new BufferedInputStream(input_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(output_socket.getOutputStream());) {
                    int cnt = -1;
                    byte[] buffer = new byte[1024];
                    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                        byte[] data = new byte[cnt];
                        System.arraycopy(buffer, 0, data, 0, cnt);
                        String sendData = httpdRequestData(data, output_socket.getInetAddress().getHostAddress() + ":" + output_socket.getPort());
                        
                        System.err.println(Thread.currentThread() + "\n" + input_socket.getInetAddress() + ":" + input_socket.getPort() + "[" + sendData);
                        //debugLog(buffer, cnt);
                        //System.err.println(sendData);
                        
                        out.write( sendData.getBytes() );
                        out.flush();
                        
                    }
                } catch (IOException e) {
                    //nop
                }
            }
        };
        return new Thread(r);
    }
    
    public static String httpdRequestData(byte[] data, String url) throws IOException {
        String contents = byte2String(data, 0, data.length);
        
        
        StringBuffer header = new StringBuffer();
        header.append("POST ").append(url).append(" HTTP/1.1\r\n");
        header.append("User-Agent: SOCKS-over-HTTP\r\n");
        header.append("Accept-Encoding: gzip,deflate\r\n");
        header.append("Keep-Alive: 300\r\n");
        header.append("Connection: keep-alive\r\n");
        header.append("Content-Type: text/plain\r\n");
        header.append("Content-Length: ").append(contents.length()).append("\r\n");
        header.append("\r\n");

        return header.toString() + contents;
    }
    
    private static String byte2String(byte[] data, int pos, int cnt) throws IOException {
        StringBuffer contents = new StringBuffer();
        for (int i = pos; i < cnt-pos; i++) {
            contents.append(String.format("0x%02x", (data[i] & 0xff)));
        }

        return contents.toString();
    }
}
