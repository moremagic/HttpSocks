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
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

/**
 *
 * @author mitsu
 */
public class HttpSocks {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        proxy();
    }

    public static void proxy() {
        int port = 55110;
        String socks_host = "192.168.1.6";
        int socks_port = 49153;

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
            System.out.print("0x" + Integer.toHexString(data[i] & 0xff));
            System.out.print(".");
        }
        System.out.println();
    }

    public void httpdRequest(byte[] data) throws IOException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL("").openConnection();
            try(OutputStream out = new BufferedOutputStream(con.getOutputStream())){
                out.write(data);
            }
        } finally {
            if(con != null)con.disconnect();
        }
    }
    
    public void httpdServer() throws IOException {
        int port = 55111;
        ServerSocket server = new ServerSocket(port);
        
        while(true){
            Socket soc = server.accept();
            try(InputStream in = soc.getInputStream()){
                int cnt = -1;
                byte[] buffer = new byte[1024];
                while((cnt = in.read(buffer, 0, buffer.length)) != -1){
                    System.out.print( new String(buffer, 0, cnt) );
                }
            }
        }
        
        
    }    
}
