/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import thrift.TRole;
import thrift.TUser;
import thrift.TUserResult;
import thrift.TUserRoleResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class AuthServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    protected static final String ROLE_ADMIN = "Admin";
    protected static final String ROLE_OWNER_CAR = "OwnerCar";
    public static final String ATTR_USER = "auth.user";
    public static final String ATTR_SESSION_ID = "auth.sessionId";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        this.setCorsHeader(resp);
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            super.service(req, resp);
            return;
        }

        TUserResult result = getAuthenticatedUser(req, resp);
        if (Err.isFail(result.getError())) {
            if (Err.isNetworkError(result.getError())) {
                _Logger.error("Service unavailable");
                fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, result.getError(), "Lỗi server");
            } else {
                fail(resp, HttpServletResponse.SC_UNAUTHORIZED, Err.FAIL, "Bạn chưa đăng nhập");
            }
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
    
    protected boolean hasRole(HttpServletRequest req, HttpServletResponse resp, String role)
    {
        TUser user = getUserFromRequest(req);
        if(user != null)
        {
            TUserRoleResult result = ClientHolder.get().getUserRole(user.getUserId());
            if(Err.isNetworkError(result.getError()))
            {
                _Logger.error("Network error, check role " + role);
                fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                return false;
            }
            
            if(result.getValue() != null && result.getValue().isIsSuperAdmin()) return true;
            
            if(Err.isSuccess(result.getError()) && result.getValue() != null)
            {
                List<TRole> roles = result.getValue().getRoles() == null ? null : result.getValue().getRoles();
                if(roles != null)
                {
                    for (TRole r : roles) {
                        if (role.equals(r.getName())) {
                            return true;
                        }
                    }
                }
            }
        }
        _Logger.info("Forbidden, userId=" + (user == null ? "unknown" : user.getUserId())
                + ", " + req.getMethod() + " " + req.getRequestURI());
        fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN,
                "Bạn không có quyền thực hiện hành động này");
        return false;
    }
}
