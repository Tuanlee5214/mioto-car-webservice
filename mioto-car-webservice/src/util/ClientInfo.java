/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import javax.servlet.http.HttpServletRequest;
import thrift.TLoginInfo;

/**
 *
 * @author tuanlee
 */
public class ClientInfo {
    
    public static TLoginInfo from(HttpServletRequest request)
    {
        String userAgent = request.getHeader("User-Agent");
        TLoginInfo info = new TLoginInfo();
        info.setUserAgent(userAgent);
        info.setUserIP(ip(request));
        info.setLongSession(false);
        return info;
    }
    
    public static String ip(HttpServletRequest request)
    {
        return request.getRemoteAddr();
    }
}
