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
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.arnx.jsonic.JSON;

/**
 *
 * @author mitsu
 */
public class MultiHostSender {

    /**
     *
     * @param request
     * @param host_list
     * @return
     * @throws IOException
     */
    public static byte[] createMultiHostResponse(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        // GET /v1.21/version HTTP/1.1
        String sURL = request.substring(0, request.indexOf("\r\n")).split(" ")[1];
        if (sURL.contains("?")) {
            sURL = sURL.substring(0, sURL.lastIndexOf("?"));
        }
        System.out.println(">> url : [" + sURL + "]");

        byte[] ret = null;
        if (sURL.endsWith("/info")) {
            //docker info
            ret = getInfo(request, host_list);
        } else if (sURL.endsWith("/version")) {
            //docker version
            ret = getVersions(request, host_list);
        } else if (sURL.endsWith("/containers/json")) {
            //docker ps
            ret = getPs(request, host_list);
        } else if (sURL.endsWith("containers/create")) {
            //docker run
            ret = getCreates(request, host_list);
        } else {
            System.err.println("no such API command.");
        }

        return ret;
    }
    
    private static byte[] getInfo(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        Map verMap = null;
        for (int i = 0; i < host_list.size(); i++) {
            String resp = getWebAPIResponce("http://" + host_list.get(i).host + ":" + host_list.get(i).port + "/v1.21/info", "GET");

            if (verMap == null) {
                verMap = (Map) JSON.decode(resp);
            } else {
                Map mm = (Map) JSON.decode(resp);
                for (Object entry : mm.entrySet()) {
                    Map.Entry mm_ent = (Map.Entry) entry;
                    if( mm_ent.getValue() instanceof List ){
                        List ll = new ArrayList();
                        
                        Object[] base = ((List)verMap.get(mm_ent.getKey())).toArray();
                        Object[] marge = ((List)verMap.get(mm_ent.getKey())).toArray();
                        for(int j = 1 ; j < base.length ; j++){
                            Object[] baselist = ((List)base[j]).toArray();
                            Object[] margelist = ((List)base[j]).toArray();
                            ll.add( Arrays.asList(new String[]{baselist[0].toString(), baselist[1] + " " + margelist[1]}) );
                        }
                        
                        verMap.put(mm_ent.getKey(), ll);
                    }else{
                        verMap.put(mm_ent.getKey(), verMap.get(mm_ent.getKey()) + " " + mm_ent.getValue());
                    }
                }
            }
        }

        String ver_data = JSON.encode(verMap);
        return ("HTTP/1.1 200 OK\n"
                + "Content-Type: application/json\n"
                + "Server: Docker/1.9.1 (linux)\n"
                + "Date: Wed, 09 Dec 2015 09:48:32 GMT\n"
                + "Content-Length: " + (ver_data.length()+1) + "\n\n" + ver_data + "\n").getBytes();
    }
    
    private static byte[] getVersions(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        Map verMap = null;
        for (int i = 0; i < host_list.size(); i++) {
            String resp = getWebAPIResponce("http://" + host_list.get(i).host + ":" + host_list.get(i).port + "/version", "GET");

            if (verMap == null) {
                verMap = (Map) JSON.decode(resp);
            } else {
                Map mm = (Map) JSON.decode(resp);
                for (Object entry : mm.entrySet()) {
                    Map.Entry mm_ent = (Map.Entry) entry;
                    verMap.put(mm_ent.getKey(), verMap.get(mm_ent.getKey()) + " " + mm_ent.getValue());
                }
            }
        }

        String ver_data = JSON.encode(verMap);
        return ("HTTP/1.1 200 OK\n"
                + "Content-Type: application/json\n"
                + "Server: Docker/1.9.1 (linux)\n"
                + "Date: Wed, 09 Dec 2015 09:48:32 GMT\n"
                + "Content-Length: " + (ver_data.length()+1) + "\n\n" + ver_data + "\n").getBytes();
    }

    private static byte[] getPs(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        String sHeader = null;
        List<String> retList = new ArrayList<>();
        for (int i = 0; i < host_list.size(); i++) {
            Socket soc = new Socket(host_list.get(i).host, host_list.get(i).port);
            String resp = new String(sendRequest(request.getBytes(), soc));
            if (sHeader == null) {
                sHeader = resp.split(new String(ProxyUtils._HttpSeparator))[0];
            }

            String sData = resp.split(new String(ProxyUtils._HttpSeparator))[1].split("\n")[1];
            retList.addAll((List) JSON.decode(sData));
        }

        String ret = sHeader
                + new String(ProxyUtils._HttpSeparator)
                + Integer.toHexString(JSON.encode(retList).length() + 1) + "\r\n"
                + JSON.encode(retList)
                + new String(ProxyUtils._HttpSeparator)
                + "0"
                + new String(ProxyUtils._HttpSeparator);
        return ret.getBytes();
    }

    private static byte[] getCreates(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        Socket soc = new Socket(host_list.get(0).host, host_list.get(0).port);
        return sendRequest(request.getBytes(), soc);
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

                if (ProxyUtils.isResponseEnd(response)) {
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

    private static String getWebAPIResponce(String sURL, String method) throws MalformedURLException, ProtocolException, IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(sURL).openConnection();
        con.setRequestMethod(method);

        StringBuffer ret = new StringBuffer();
        try (InputStream in = con.getInputStream()) {
            int cnt = 0;
            byte[] buffer = new byte[1024];
            while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
                ret.append(new String(buffer, 0, cnt));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return ret.toString();
    }
}
