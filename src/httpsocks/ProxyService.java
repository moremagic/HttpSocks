/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpsocks;


import httpsocks.http.Socks2HttpClient;
import httpsocks.http.Socks2HttpServer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author mitsu
 */
public class ProxyService {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /**
         * plain proxy *
         */
        //new ProxyService().proxy(55110, "192.168.56.102", 1080);

        /**
         * multi plain proxy *
         */
        //        new Thread(){
        //            public void run(){
        //                new ProxyService().proxy(55110, "localhost", 55111);
        //            }
        //        }.start();
        //        new Thread(){
        //            public void run(){
        //                new ProxyService().proxy(55111, "192.168.56.102", 1080);
        //            }
        //        }.start();
        /**
         * http socks *
         */
         new Thread() {
            public void run() {
                new Socks2HttpClient().proxy(55110, "localhost", 55111);
            }
        }.start();
        new Thread() {
            public void run() {
                new Socks2HttpServer().proxy(55111, "192.168.56.102", 1080);
            }
        }.start();
        
        
//        /**
//         * socks 2 string
//         */
//        new Thread() {
//            public void run() {
//                new Socks2StringClient().proxy(55110, "localhost", 55111);
//            }
//        }.start();
//        new Thread() {
//            public void run() {
//                new Socks2StringServer().proxy(55111, "192.168.56.102", 1080);
//            }
//        }.start();


    }

    public void proxy(int port, String socks_host, int socks_port) {
        try {
            ServerSocket server = new ServerSocket(port);

            while (true) {
                Socket in_socket = server.accept();
                Socket out_socket = new Socket(socks_host, socks_port);

                createSendThread(in_socket, out_socket).start();
                createReceiveThread(out_socket, in_socket).start();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public Thread createSendThread(final Socket input_socket, final Socket output_socket) throws IOException {
        return createThread(input_socket, output_socket);
    }

    public Thread createReceiveThread(final Socket input_socket, final Socket output_socket) throws IOException {
        return createThread(input_socket, output_socket);
    }

    private static final Thread createThread(final Socket input_socket, final Socket output_socket) throws IOException {
        Runnable r = new Runnable() {
            public void run() {
                try (InputStream in = new BufferedInputStream(input_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(output_socket.getOutputStream());) {

                    int cnt = -1;
                    byte[] buffer = new byte[1024];
                    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                        debugLog(buffer, cnt);
                        out.write(buffer, 0, cnt);
                        out.flush();
                    }
                } catch (IOException e) {
                    //nop
                }
            }
        };
        return new Thread(r);
    }

    public static void debugLog(byte[] data, int cnt) {
        //DEBUG LOG
        for (int i = 0; i < cnt; i++) {
            System.out.print(String.format("0x%02x", (data[i] & 0xff)));
        }
        System.out.println();
    }
}
