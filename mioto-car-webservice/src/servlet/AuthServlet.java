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
        
        this.setCorsHeader(resp);
        if("OPTIONS".equalsIgnoreCase(req.getMethod()))
        {
            super.service(req, resp);
            return;
        }
        
        TUserResult result = getAuthenticatedUser(req, resp);
        if(Err.isFail(result.getError()))
        {
            if(Err.isNetworkError(result.getError()))
            {
                _Logger.error("Service unavailable");
                fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, result.getError(), "service unavailable");
            }
            else fail(resp, HttpServletResponse.SC_UNAUTHORIZED, Err.FAIL, "unauthorized");
            return;
        }

        //req.setAttribute(ATTR_SESSION_ID, Long.valueOf(sessionId));
        req.setAttribute(ATTR_USER, new TUser(result.getValue()));

        super.service(req, resp);                      
    }

    protected TUser getUserFromRequest(HttpServletRequest req) {
        TUser v = (TUser) req.getAttribute(ATTR_USER);
        return v == null ? null : new TUser(v);
    }
    

}
