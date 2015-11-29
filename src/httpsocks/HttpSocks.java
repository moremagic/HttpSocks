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
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author mitsu
 */
public class HttpSocks {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if(args.length == 0 || args.length != 3){
            System.err.println("Usage: java httpssocks.HttpSocks [listen] [host] [port]");
            System.err.println("  httpssocks.HttpSocks 192.168.1.6 2375");
            System.exit(-1);
        }
        //proxy(55110, "192.168.1.6", 49153);
        proxy(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
    }

    public static void proxy(int port, String socks_host, int socks_port) {
        try {
            ServerSocket server = new ServerSocket(port);

            while (true) {
                Socket in_socket = server.accept();
                Socket out_socket = new Socket(socks_host, socks_port);

                createThread(in_socket, out_socket).start();
                createThread(out_socket, in_socket).start();
            }

        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    public static Thread createThread(final Socket input_socket, final Socket output_socket) throws IOException {
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
                } catch (Exception err) {
                    //err.printStackTrace();
                }
            }
        };
        return new Thread(r);
    }

    public static void debugLog(byte[] data, int cnt) {
        //DEBUG LOG
        /*
        for (int i = 0; i < cnt; i++) {
            System.out.print("0x" + Integer.toHexString(data[i] & 0xff));
        }
        */
        System.out.print(new String(data, 0 ,cnt));
        System.out.println();
    }
}
