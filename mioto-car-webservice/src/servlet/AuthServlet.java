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
import thrift.TSessionResult;
import util.ClientHolder;
import util.CookieSigner;
import util.CookieUtil;

/**
 *
 * @author tuanlee
 */
public class AuthServlet extends BaseServlet {
    
    private static final long serialVersionUID = 1L;

    public static final String ATTR_USER_ID    = "auth.userId";
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

        TSessionResult sr = ClientHolder.get().getSession(sessionId);
        if (Err.isFail(sr.getError()) || sr.getValue() == null) {
            if (Err.isNetworkError(sr.getError())) {
                // distinguish these in the LOG, but not in the response
                _Logger.error("session lookup failed, err=" + sr.getError());
                fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, sr.getError(),
                        "service unavailable");
            } else {
                CookieUtil.clear(resp);                
                fail(resp, HttpServletResponse.SC_UNAUTHORIZED, Err.FAIL, "unauthorized");
            }
            return;
        }

        req.setAttribute(ATTR_SESSION_ID, Long.valueOf(sessionId));
        req.setAttribute(ATTR_USER_ID, Integer.valueOf(sr.getValue().getUserId()));

        super.service(req, resp);                      
    }

    protected int userId(HttpServletRequest req) {
        Integer v = (Integer) req.getAttribute(ATTR_USER_ID);
        return v == null ? 0 : v.intValue();
    }

}
