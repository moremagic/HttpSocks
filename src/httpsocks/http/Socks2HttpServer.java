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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mitsu
 */
public class Socks2HttpServer extends ProxyService {

    @Override
    public Thread createSendThread(Socket input_socket, Socket output_socket) throws IOException {
        return http2socks(input_socket, output_socket);
    }

    private static Thread http2socks(final Socket input_socket, final Socket output_socket) throws IOException {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try (InputStream in = new BufferedInputStream(input_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(output_socket.getOutputStream());) {

                    int split_cnt = 0;
                    byte[] split = "\r\n\r\n".getBytes();
                    boolean bData = false;

                    StringBuffer sss = new StringBuffer();
                    
                    int cnt = -1;
                    int context_length = 0;
                    byte[] buffer = new byte[1024];
                    
                    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                        //System.out.println(input_socket.getInetAddress() + ":" + input_socket.getPort() + "[" + new String(buffer, 0, cnt) + "]");
                        for (int i = 0; i < cnt; i++) {
                            if (bData) {
                                String data = new String(buffer, i, cnt - i);
                                System.out.println(output_socket.getInetAddress() + ":" + output_socket.getPort() + "[send] >> " + data + ";");

                                sss.append(data);
                                

                                context_length -= data.length();
                                if(context_length == 0){
                                    out.write(string2byte(sss.toString()));
                                    out.flush();
                                
                                    bData = false;
                                    split_cnt = 0;
                                    context_length = 0;
                                    sss.setLength(0);
                                }
                                break;
                            } else {
                                if (buffer[i] == split[split_cnt]) {
                                    split_cnt++;
                                } else {
                                    split_cnt = 0;
                                }

                                if (split_cnt == split.length) {
                                    context_length = getContentLength(new String(buffer, 0, i));
                                    bData = true;
                                }
                            }
                        }
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
        for (int i = 0; i < respons.length() - 2; i += 4) {
            ret[i / 4] = (byte) (Integer.decode(respons.substring(i, i + 4)) & 0xff);
        }
        return ret;
    }

    public static int getContentLength(String header){
        int start_idx = header.indexOf("Content-Length:");
        if(start_idx == -1){
            return 0;
        }else{
            return Integer.parseInt(header.substring(start_idx + "Content-Length:".length(), header.indexOf("\r\n", start_idx)).trim());
        }
    }
    
    /**
     * debug & test
     *
     * @param argv
     */
    public static void main(String[] argv) {
        String data = "0x040x010x000x500xd80x3a0xdd0x0e0x00";

        byte[] byteData = string2byte(data);

        System.out.println(data);
        debugLog(byteData, byteData.length);

        try {
            System.out.println(data.equals(Socks2HttpClient.byte2String(byteData, 0, byteData.length)) ? "正常終了" : "失敗");
        } catch (IOException ex) {
            Logger.getLogger(Socks2HttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
