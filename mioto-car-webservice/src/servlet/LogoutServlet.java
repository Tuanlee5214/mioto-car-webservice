/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.ClientHolder;
import util.CookieSigner;
import util.CookieUtil;

/**
 *
 * @author tuanlee
 */
public class LogoutServlet extends BaseServlet{
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    {
        try
        {
            long sessionId = CookieSigner.verify(CookieUtil.read(req));
            if(sessionId > 0)
            {
                ClientHolder.get().logout(sessionId);
            }
            CookieUtil.clear(resp);
            ok(resp, "Đã đăng xuất");
        }
        catch(Exception e)
        {
            _Logger.error("logged out failed", e);
            CookieUtil.clear(resp);
            ok(resp, "Đã đăng xuất");
        }
    }
}
