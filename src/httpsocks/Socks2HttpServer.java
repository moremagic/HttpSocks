/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpsocks;

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
public abstract class Socks2HttpServer extends ProxyService {

    @Override
    abstract public Thread createSendThread(final Socket input_socket, final Socket output_socket) throws IOException;
    @Override
    abstract public Thread createReceiveThread(final Socket input_socket, final Socket output_socket) throws IOException;

    public static Socks2HttpServer createClient(){
        return new Socks2HttpServer(){
            @Override
            public Thread createSendThread(Socket input_socket, Socket output_socket) throws IOException {
                return socks2https(input_socket, output_socket);
            }

            @Override
            public Thread createReceiveThread(Socket input_socket, Socket output_socket) throws IOException {
                return http2socks(output_socket, input_socket);
            }
        };
    }

    public static Socks2HttpServer createServer(){
        return new Socks2HttpServer(){
            @Override
            public Thread createSendThread(Socket input_socket, Socket output_socket) throws IOException {
                return http2socks(input_socket, output_socket);
            }

            @Override
            public Thread createReceiveThread(Socket input_socket, Socket output_socket) throws IOException {
                return socks2https(output_socket, input_socket);
            }
        };
    }
        
    /**
     * socks -> http
     *
     * @param input_socket
     * @param output_socket
     * @return
     * @throws IOException
     */
    private static Thread socks2https(final Socket input_socket, final Socket output_socket) throws IOException {
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

    /**
     * http -> socks
     *
     * @param input_socket
     * @param output_socket
     * @return
     * @throws IOException
     */
    private static Thread http2socks(final Socket input_socket, final Socket output_socket) throws IOException {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try (InputStream in = new BufferedInputStream(input_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(output_socket.getOutputStream());) {
                    
                    int split_cnt = 0;
                    byte[] split = "\r\n\r\n".getBytes();
                    boolean bData = false;
                    
                    
                    int cnt = -1;
                    byte[] buffer = new byte[1024];
                    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                        //System.out.println(input_socket.getInetAddress() + ":" + input_socket.getPort() + "[" + new String(buffer, 0, cnt) + "]");
                        for(int i = 0 ; i < cnt ; i++){
                            if( bData ){
                                String ssss = new String(buffer, i, cnt-i);
                                System.out.println(output_socket.getInetAddress() + ":" + output_socket.getPort() + "[send] >> " + ssss + ";");
                                
                                out.write(responsData(ssss));
                                out.flush();
                                
                                bData=false;split_cnt = 0;
                                break;
                            }else{
                                if(buffer[i] == split[split_cnt]){
                                    split_cnt++;
                                }else{
                                    split_cnt = 0;
                                }
                                
                                if(split_cnt == split.length){
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

    public static String httpdRequestData(byte[] data, String url) throws IOException {
        StringBuffer header = new StringBuffer();
        header.append("POST ").append(url).append(" HTTP/1.1\r\n");
        header.append("User-Agent: SOCKS-over-HTTP\r\n");
        header.append("Accept-Encoding: gzip,deflate\r\n");
        header.append("Keep-Alive: 300\r\n");
        header.append("Connection: keep-alive\r\n");
        header.append("Content-Type: text/plain\r\n");
        header.append("Content-Length: ").append(data.length*4).append("\r\n");
        header.append("\r\n");

        StringBuffer contents = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            contents.append(String.format("0x%02x", (data[i] & 0xff)));
        }

        return header.toString() + contents.toString();
    }
    
    public static byte[] responsData(String respons){
        byte[] ret = new byte[respons.length()/4];
        for(int i = 0; i < ret.length-1 ; i += 4){
            ret[i/4] = Byte.decode(respons.substring(i, i+4));
        }
        return ret;
    }
}
