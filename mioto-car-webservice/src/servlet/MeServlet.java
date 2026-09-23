/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import thrift.TUser;

/**
 *
 * @author tuanlee
 */
public class MeServlet extends AuthServlet{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    {
        TUser user = this.getUserFromRequest(req);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("user", user);
        ok(resp, data);
    }
}
