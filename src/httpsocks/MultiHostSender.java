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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
        String sMethod = request.substring(0, request.indexOf("\r\n")).split(" ")[0];
        String sURL = request.substring(0, request.indexOf("\r\n")).split(" ")[1];
        if (sURL.contains("?")) {
            sURL = sURL.substring(0, sURL.lastIndexOf("?"));
        }

        byte[] ret = null;
        if (sMethod.equals("GET") && sURL.endsWith("/info")) {
            //docker info
            ret = getInfo(request, host_list);
        } else if (sMethod.equals("GET") && sURL.endsWith("/version")) {
            //docker version
            ret = getVersions(request, host_list);
        } else if (sMethod.equals("GET") && sURL.endsWith("/containers/json")) {
            //docker ps
            ret = getContainerList(request, host_list);
        } else if (sMethod.equals("GET") && sURL.endsWith("/images/json")) {
            //docker images
            ret = getContainerList(request, host_list);
        } else if (sMethod.equals("GET") && Pattern.compile("\\/containers\\/.+\\/json$").matcher(sURL).find()) {
            //docker container info
            ret = getContainerInfo(request, host_list);
        } else if (sMethod.equals("GET") && Pattern.compile("\\/images\\/.+\\/json$").matcher(sURL).find()) {
            //docker container info
            ret = getContainerInfo(request, host_list);
        } else if (sMethod.equals("GET") && sURL.endsWith("/images/search")) {
            //docker search
            // かならず１つめのホストを使用して検索を行う
            ret = plainRequest(request, host_list.get(0));
        } else if (sMethod.equals("DELETE") && sURL.startsWith("/containers/")) {
            //docker delete
            ret = deleteContainer(request, host_list);
        } else if (sMethod.equals("POST") && sURL.endsWith("/containers/create")) {
            //docker container create
            ret = getConteinarCreate(request, getHasMargineHostInfoSortedList(host_list));
        } else if (sMethod.equals("POST") && Pattern.compile("\\/containers\\/.+\\/start").matcher(sURL).find()) {
            //docker container strart
            ret = startStopContainer(request, host_list);
        } else if (sMethod.equals("POST") && Pattern.compile("\\/containers\\/.+\\/stop").matcher(sURL).find()) {
            //docker container stop
            ret = startStopContainer(request, host_list);
        } else {
            System.err.println("no such API command.");
            //ret = plainRequest(request, host_list);
        }

        System.out.println(">> url : [" + sURL + "]");
        if(ret != null)System.out.println("----------<<create response>>----------\n" + new String(ret) + "\n------------------------------");
        else System.out.println("----------<<create response>>----------\n null \n------------------------------");

        return ret;
    }

    /**
     * docker info response create
     *
     * @param request
     * @param host_list
     * @return
     * @throws IOException
     */
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
                    if (mm_ent.getValue() instanceof List) {
                        List ll = new ArrayList();

                        Object[] base = ((List) verMap.get(mm_ent.getKey())).toArray();
                        Object[] marge = ((List) verMap.get(mm_ent.getKey())).toArray();
                        for (int j = 1; j < base.length; j++) {
                            Object[] baselist = ((List) base[j]).toArray();
                            Object[] margelist = ((List) marge[j]).toArray();

                            ll.add(Arrays.asList(new String[]{baselist[0].toString(), ProxyUtils.appendObject(baselist[1], margelist[1])}));
                        }

                        verMap.put(mm_ent.getKey(), ll);
                    } else {
                        // info レスポンスをマージすると Dockerクライアントが正常に読めなくなるのでこの部分のデータはマージしない
                        //verMap.put(mm_ent.getKey(), ProxyUtils.appendObject( verMap.get(mm_ent.getKey()), mm_ent.getValue()));
                    }
                }
            }
        }

        String ver_data = JSON.encode(verMap);
        return (ProxyUtils.createHttResponceHeader200("1.9.1")
                + "Content-Length: " + (ver_data.length() + 1) + "\n\n" + ver_data + "\n").getBytes();
    }

    /**
     * docker version response create
     *
     * @param request
     * @param host_list
     * @return
     * @throws IOException
     */
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
        return (ProxyUtils.createHttResponceHeader200("1.9.1")
                + "Content-Length: "
                + (ver_data.length() + 1)
                + "\n\n"
                + ver_data + "\n").getBytes();
    }

    /**
     * (docker ps | docker images) response create
     *
     * @param request
     * @param host_list
     * @return
     * @throws IOException
     */
    private static byte[] getContainerList(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        List<String> retList = new ArrayList<>();
        for (int i = 0; i < host_list.size(); i++) {
            String sData = getWebAPIResponce("http://" + host_list.get(i).host + ":" + host_list.get(i).port + request.substring(0, request.indexOf("\r\n")).split(" ")[1], "GET");
            retList.addAll((List) JSON.decode(sData));
        }
        String ret = ProxyUtils.createHttResponceHeader200("1.9.1")
                + "Transfer-Encoding: chunked\n\n"
                + Integer.toHexString(JSON.encode(retList).length() + 1) + "\r\n"
                + JSON.encode(retList)
                + new String(ProxyUtils._HttpSeparator)
                + "0"
                + new String(ProxyUtils._HttpSeparator);
        return ret.getBytes();
    }

    
    /**
     * (docker start) response create
     *
     * @param request
     * @param host_list
     * @return
     * @throws IOException
     */
    private static byte[] startStopContainer(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        for (int i = 0; i < host_list.size(); i++) {
            try {
                String sData = getWebAPIResponce("http://" + host_list.get(i).host + ":" + host_list.get(i).port + request.substring(0, request.indexOf("\r\n")).split(" ")[1], "POST");
            } catch (IOException err) {
                //nop
            }
        }

        String ret = ProxyUtils.createHttResponceHeader204("1.9.1");
        return ret.getBytes();
    }    
    
    /**
     * (docker delete) response create
     *
     * @param request
     * @param host_list
     * @return
     * @throws IOException
     */
    private static byte[] deleteContainer(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        for (int i = 0; i < host_list.size(); i++) {
            try {
                String sData = getWebAPIResponce("http://" + host_list.get(i).host + ":" + host_list.get(i).port + request.substring(0, request.indexOf("\r\n")).split(" ")[1], "DELETE");
            } catch (IOException err) {
                //nop
            }
        }

        String ret = ProxyUtils.createHttResponceHeader204("1.9.1");
        return ret.getBytes();
    }

    /**
     * (docker info | docker images info) response create
     *
     * @param request
     * @param host_list
     * @return
     * @throws IOException
     */
    private static byte[] getContainerInfo(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        Map retMap = null;
        for (int i = 0; i < host_list.size(); i++) {
            try {
                String sData = getWebAPIResponce("http://" + host_list.get(i).host + ":" + host_list.get(i).port + request.substring(0, request.indexOf("\r\n")).split(" ")[1], "GET");
                if (!sData.trim().isEmpty()) {
                    retMap = ((Map) JSON.decode(sData));
                }
            } catch (IOException err) {
                //nop
            }
        }

        String ret = ProxyUtils.createHttResponceHeader200("1.9.1")
                + "Transfer-Encoding: chunked\n\n"
                + Integer.toHexString(JSON.encode(retMap).length() + 1) + "\r\n"
                + JSON.encode(retMap)
                + new String(ProxyUtils._HttpSeparator)
                + "0"
                + new String(ProxyUtils._HttpSeparator);
        return ret.getBytes();
    }

    /**
     * 余力の残っているホストを取得する
     * @param host_list
     * @return
     * @throws IOException 
     */
    private static List<HttpSocks._DockerHostInfo> getHasMargineHostInfoSortedList(List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        Map<Integer, HttpSocks._DockerHostInfo> host_map = new HashMap<>();
        for (int i = 0; i < host_list.size(); i++) {
            String sData = getWebAPIResponce("http://" + host_list.get(i).host + ":" + host_list.get(i).port + "/containers/json", "GET");
            host_map.put(((List) JSON.decode(sData)).size(), host_list.get(i));
        }
        
        List<Integer> keyList = new ArrayList(host_map.keySet());
        Collections.sort(keyList);
        
        List<HttpSocks._DockerHostInfo> ret = new ArrayList<>();
        for(Integer key :keyList){
            ret.add(host_map.get(key));
        }
        
        
        System.out.println(keyList);
        return ret;
    }    
    
    /**
     * docker create response create
     *
     * @param request
     * @param host_list
     * @return
     * @throws IOException
     */
    private static byte[] getConteinarCreate(String request, List<HttpSocks._DockerHostInfo> host_list) throws IOException {
        String sData = "";
        for (int i = 0; i < host_list.size(); i++) {
            try {
                sData = new String( plainRequest(request, host_list.get(i)) );
                if( sData.split("\r\n")[0].contains("HTTP/1.1 201 Created")){
                    return sData.getBytes();
                }
            } catch (IOException err) {
                //nop
                err.printStackTrace();
            }
        }
        
        throw new IOException("error create;");
    }

    private static byte[] plainRequest(String request, HttpSocks._DockerHostInfo host_info) throws IOException {
        Socket soc = new Socket(host_info.host, host_info.port);
        return sendRequest(request.getBytes(), soc);
    }

    /**
     * リクエスト送信部
     *
     * @param request
     * @param soc
     * @return
     */
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

    /**
     * WebAPI 実行用ユーティリティ
     *
     * @param sURL
     * @param method
     * @return
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException
     */
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
        }
        return ret.toString();
    }
}
