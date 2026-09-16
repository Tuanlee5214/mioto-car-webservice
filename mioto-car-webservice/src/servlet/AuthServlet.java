/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import thrift.TUser;
import thrift.TUserResult;
import util.ClientHolder;
import util.CookieSigner;
import util.CookieUtil;

/**
 *
 * @author tuanlee
 */
public class AuthServlet extends BaseServlet {
    
    private static final long serialVersionUID = 1L;

    public static final String ATTR_USER    = "auth.user";
    public static final String ATTR_SESSION_ID = "auth.sessionId";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        long sessionId = CookieSigner.verify(CookieUtil.read(req));
        if (sessionId <= 0) {
            fail(resp, HttpServletResponse.SC_UNAUTHORIZED, Err.FAIL, "unauthorized");
            return;                                   
        }

        TUserResult sr = ClientHolder.get().getUserBySessionId(sessionId);
        if (Err.isFail(sr.getError()) || sr.getValue() == null) {
            if (Err.isNetworkError(sr.getError())) {
                _Logger.error("session lookup failed, err=" + sr.getError());
                fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, sr.getError(),
                        "service unavailable");
            } else {
                CookieUtil.clear(resp);                
                fail(resp, HttpServletResponse.SC_UNAUTHORIZED, Err.FAIL, "unauthorized");
            }
            return;
        }

        //req.setAttribute(ATTR_SESSION_ID, Long.valueOf(sessionId));
        req.setAttribute(ATTR_USER, new TUser(sr.getValue()));

        super.service(req, resp);                      
    }

    protected TUser getUserFromRequest(HttpServletRequest req) {
        TUser v = (TUser) req.getAttribute(ATTR_USER);
        return v == null ? null : new TUser(v);
    }

}
