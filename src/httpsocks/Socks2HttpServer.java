/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpsocks;

import static httpsocks.ProxyService.debugLog;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * ○ socks <-> http http <-> socks
 *
 * @author mitsu
 */
public class Socks2HttpServer extends ProxyService {

    /**
     * socks -> http
     *
     * @param input_socket
     * @param output_socket
     * @return
     * @throws IOException
     */
    public Thread createSendThread(final Socket input_socket, final Socket output_socket) throws IOException {
        Runnable r = new Runnable() {
            public void run() {
                try (InputStream in = new BufferedInputStream(input_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(output_socket.getOutputStream());) {
                    int cnt = -1;
                    byte[] buffer = new byte[1024];
                    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                        //System.err.println(input_socket.getInetAddress() + ":" + output_socket.getPort() + "[" + new String(buffer, 0, cnt));
                        //debugLog(buffer, cnt);

                        byte[] data = new byte[cnt];
                        System.arraycopy(buffer, 0, data, 0, cnt);
                        
                        out.write( httpdRequestData(data, output_socket.getInetAddress().getHostAddress() + ":" + output_socket.getPort()).getBytes() );
                        out.flush();
                    }
                } catch (IOException e) {
                    //nop
                }
            }
        };
        return new Thread(r);
    }

    /**
     * http -> socks
     *
     * @param input_socket
     * @param output_socket
     * @return
     * @throws IOException
     */
    public Thread createReceiveThread(final Socket input_socket, final Socket output_socket) throws IOException {
        Runnable r = new Runnable() {
            public void run() {
                try (InputStream in = new BufferedInputStream(input_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(output_socket.getOutputStream());) {
                    
                    StringBuilder respons = new StringBuilder();
                    
                    int cnt = -1;
                    byte[] buffer = new byte[1024];
                    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                        debugLog(buffer, cnt);
                        respons.append(new String(buffer, 0, cnt));
                    }
                    
                    byte[] data = responsData(respons.toString().substring( respons.toString().indexOf("\r\n\r\n")));
                    out.write( data );
                    out.flush();
                } catch (IOException e) {
                    //nop
                }
            }
        };
        return new Thread(r);
    }

    public String httpdRequestData(byte[] data, String url) throws IOException {
        StringBuilder header = new StringBuilder();
        header.append("POST " + url + " HTTP/1.1\r\n");
        header.append("User-Agent: SOCKS-over-HTTP\r\n");
        header.append("Accept-Encoding: gzip,deflate\r\n");
        header.append("Keep-Alive: 300\r\n");
        header.append("Connection: keep-alive\r\n");
        header.append("Content-Type: text/plain\r\n");
        header.append("Content-Length: ").append(data.length*4).append("\r\n");
        header.append("\r\n");

        StringBuilder contents = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            contents.append(String.format("0x%02x", new Integer(data[i] & 0xff)));
        }

        return header.toString() + contents.toString();
    }
    
    public byte[] responsData(String respons){
        byte[] ret = new byte[respons.length()/4];
        for(int i = 0; i < respons.length() ; i += 4){
            ret[i/4] = Byte.parseByte(respons.substring(i, i+4));
        }
        return ret;
    }
}
