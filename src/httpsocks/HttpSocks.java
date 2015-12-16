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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mitsu
 */
public class HttpSocks {

    public static class _DockerHostInfo{
        public static final List<_DockerHostInfo> HOST_ARRAYS = new ArrayList<>();
        public int port;
        public String host;

        public _DockerHostInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0 || args.length < 2) {
            System.err.println("Usage: java httpssocks.HttpSocks [listen] { [host:port], } * n");
            System.err.println("  httpssocks.HttpSocks 55110 192.168.1.6:2375,192.168.1.7:2375");
            System.exit(-1);
        }
        
        for(String hostsArray: args[1].split(",")){
            String[] hosts = hostsArray.split(":");
            _DockerHostInfo.HOST_ARRAYS.add(new _DockerHostInfo(hosts[0], Integer.parseInt(hosts[1])));
        }
        proxy(Integer.parseInt(args[0]));
    }

    public static void proxy(int port) {
        try {
            ServerSocket server = new ServerSocket(port);

            while (true) {
                Socket server_socket = server.accept();
                createThread_MultiHost(server_socket).start();
            }

        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    public static Thread createThread(final Socket input_socket, final Socket output_socket) throws IOException {
        Runnable r = new Runnable() {
            public void run() {
                System.out.println("== open == " + this);
                try (InputStream in = new BufferedInputStream(input_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(output_socket.getOutputStream());) {
                    int cnt = -1;
                    byte[] buffer = new byte[1024];
                    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                        ProxyUtils.debugLog(buffer, cnt);
                        out.write(buffer, 0, cnt);
                        out.flush();
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                } finally {
                    System.out.println("== close ==" + this);
                }
            }
        };
        return new Thread(r);
    }

    public static Thread createThread_MultiHost(final Socket server_socket) throws IOException {
        Runnable r = new Runnable() {
            public void run() {
                System.out.println("== open == " + this);
                try (InputStream in = new BufferedInputStream(server_socket.getInputStream());
                        OutputStream out = new BufferedOutputStream(server_socket.getOutputStream());){
                    
                    List<Byte> inputBuffer = new ArrayList();
                    
                    int cnt = -1;
                    byte[] read_buffer = new byte[2048];
                    while ((cnt = in.read(read_buffer, 0, read_buffer.length)) != -1) {
                        for (int i = 0; i < cnt; i++){
                            inputBuffer.add(read_buffer[i]);
                        }
                        if(ProxyUtils.containData(inputBuffer, ProxyUtils._HttpSeparator)){
                            byte [] request_data = ProxyUtils.byteList2ByteArrays(inputBuffer);
                            ProxyUtils.debugLog(request_data, request_data.length);
                            
                            
                            byte[] response = MultiHostSender.createMultiHostResponse(new String(request_data), _DockerHostInfo.HOST_ARRAYS);
                            out.write(response);
                            out.flush();
                            break;
                        }
                    }                    
                } catch (Exception err) {
                    err.printStackTrace();
                } finally {
                    System.out.println("== close ==" + this);
                }
            }

            
        };
        return new Thread(r);
    }
    


}
