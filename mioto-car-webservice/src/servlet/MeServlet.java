/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static servlet.BaseServlet._Logger;
import thrift.TRole;
import thrift.TUser;
import thrift.TUserRoleResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class MeServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        TUser user = this.getUserFromRequest(req);
        TUserRoleResult userRoleResult = ClientHolder.get().getUserRole(user.getUserId());
        if (Err.isFail(userRoleResult.getError())) {
            if (Err.isNetworkError(userRoleResult.getError())) {
                _Logger.error("Network error, get user role");
                fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                return;
            }
            _Logger.error("get user role failed");
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
            return;
        }
        List<TRole> roles = userRoleResult.getValue().getRoles();
        boolean isSuperAdmin = userRoleResult.getValue().isIsSuperAdmin();
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("user", user);
        data.put("roles", roles);
        data.put("isSuperAdmin", isSuperAdmin);
        ok(resp, data);
    }
}
