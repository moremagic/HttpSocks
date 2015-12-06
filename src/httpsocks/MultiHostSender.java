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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mitsu
 */
public class MultiHostSender {

    public static byte[] createMultiHostResponse(byte[] request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        Socket soc = new Socket(host_list.get(0).host, host_list.get(0).port);
        return sendRequest(request, soc);
    }

    private static byte[] sendRequest(byte[] request, Socket soc) {
        List<Byte> response = new ArrayList();

        try (InputStream in = new BufferedInputStream(soc.getInputStream());
                OutputStream out = new BufferedOutputStream(soc.getOutputStream());) {
            
            out.write(request);
            out.flush();

            int cnt = -1;
            byte[] read_buffer = new byte[512];
            while ((cnt = in.read(read_buffer, 0, read_buffer.length)) != -1) {
                ProxyUtils.debugLog(read_buffer, cnt);
                for (int i = 0; i < cnt; i++) {
                    response.add(read_buffer[i]);
                }
                
                
                if(ProxyUtils.isResponseEnd(response)){
                    break;
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            System.out.println("== close ==" + soc);
        }

        byte[] request_data = ProxyUtils.byteList2ByteArrays(response);
        ProxyUtils.debugLog(request_data, request_data.length);
        return request_data;
    }
}
