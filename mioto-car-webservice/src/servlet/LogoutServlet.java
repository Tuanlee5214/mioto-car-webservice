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
import wrapper.CarClientWrapper;

/**
 *
 * @author tuanlee
 */
public class LogoutServlet extends BaseServlet{
    private static final long serialVersionUID = 1;
    private CarClientWrapper _mw;
    
    @Override
    public void init()
    {
        _mw = ClientHolder.get();
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    {
        try
        {
            long sessionId = CookieSigner.verify(CookieUtil.read(req));
            if(sessionId > 0)
            {
                _mw.logout(sessionId);
            }
            CookieUtil.clear(resp);
            ok(resp, "logged out");
        }
        catch(Exception e)
        {
            _Logger.error("logged out failed", e);
            CookieUtil.clear(resp);
            ok(resp, "logged out");
        }
    }
}
