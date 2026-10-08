/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import role.Role;
import thrift.TUser;
import thrift.TUserRoleResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class UserRoleServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            TUser user = getUserFromRequest(req);
            String userIdFromParam = param(req, null, "userId");
            int userId = userIdFromParam == null ? (int) user.getUserId() : Integer.parseInt(userIdFromParam);
            if (userId <= 0) {
                _Logger.info("Bad request userId : " + userId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Người dùng không hợp lệ");
                return;
            }
            if (!hasRole(req, resp, Role.USER_ROLE_READ)) return;
            TUserRoleResult result = ClientHolder.get().getUserRole(userId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, get user role");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                _Logger.error("get user role failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("get user role failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.USER_ROLE_WRITE)) return;
            Map<String, Object> body = jsonBody(req);
            String userIdFromParam = param(req, body, "userId");
            if (userIdFromParam == null) {
                _Logger.info("Bad request userId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Người dùng không được để trống");
                return;
            }
            int userId = Integer.parseInt(userIdFromParam);
            if (userId <= 0) {
                _Logger.info("Bad request userId : " + userId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Người dùng không hợp lệ");
                return;
            }

            // roleIds: [1, 2, 3]
            Object rawRoleIds = body == null ? null : body.get("roleIds");
            if (!(rawRoleIds instanceof List) || ((List<?>) rawRoleIds).isEmpty() || ((List<?>) rawRoleIds).size() > 100) {
                _Logger.info("Bad request roleIds");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách quyền không hợp lệ (1 đến 100 quyền)");
                return;
            }
            List<Integer> roleIds = new ArrayList<Integer>();
            for (Object o : (List<?>) rawRoleIds) {
                if (!(o instanceof Number)) {
                    _Logger.info("Bad request roleId item");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Quyền không hợp lệ");
                    return;
                }
                int roleId = ((Number) o).intValue();
                if (roleId <= 0) {
                    _Logger.info("Bad request roleId : " + roleId);
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Quyền không hợp lệ");
                    return;
                }
                roleIds.add(roleId);
            }

            TUserRoleResult result = ClientHolder.get().createUserRole(userId, roleIds);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, create user role");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (result.getError() == Err.BAD_REQUEST) {
                    _Logger.info("Bad request roleIds not exist (create user role)");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Có quyền không tồn tại");
                    return;
                }
                _Logger.error("create user role failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("create user role failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    // DELETE /api/user-roles   body: {"userId": 5, "roleIds": [1, 2]}
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.USER_ROLE_WRITE)) return;
            Map<String, Object> body = jsonBody(req);
            String userIdFromParam = param(req, body, "userId");
            if (userIdFromParam == null) {
                _Logger.info("Bad request userId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Người dùng không được để trống");
                return;
            }
            int userId = Integer.parseInt(userIdFromParam);
            if (userId <= 0) {
                _Logger.info("Bad request userId : " + userId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Người dùng không hợp lệ");
                return;
            }

            // roleIds: [1, 2, 3]
            Object rawRoleIds = body == null ? null : body.get("roleIds");
            if (!(rawRoleIds instanceof List) || ((List<?>) rawRoleIds).isEmpty() || ((List<?>) rawRoleIds).size() > 100) {
                _Logger.info("Bad request roleIds");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách quyền không hợp lệ (1 đến 100 quyền)");
                return;
            }
            List<Integer> roleIds = new ArrayList<Integer>();
            for (Object o : (List<?>) rawRoleIds) {
                if (!(o instanceof Number)) {
                    _Logger.info("Bad request roleId item");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Quyền không hợp lệ");
                    return;
                }
                int roleId = ((Number) o).intValue();
                if (roleId <= 0) {
                    _Logger.info("Bad request roleId : " + roleId);
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Quyền không hợp lệ");
                    return;
                }
                roleIds.add(roleId);
            }

            TUserRoleResult result = ClientHolder.get().deleteUserRole(userId, roleIds);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete user role");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete user role");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Người dùng không có quyền này");
                    return;
                }
                _Logger.error("delete user role failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("delete user role failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}
