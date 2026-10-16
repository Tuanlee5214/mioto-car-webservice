/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import role.Role;
import thrift.TListRoleResult;
import thrift.TRole;
import thrift.TRoleResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class RoleServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.ROLE_READ)) return;
            String name = param(req, null, "name");
            String countFromParam = param(req, null, "count");
            String offsetFromParam = param(req, null, "offset");
            int count = Integer.parseInt(countFromParam == null ? "20" : countFromParam);
            int offset = Integer.parseInt(offsetFromParam == null ? "0" : offsetFromParam);
            count = (count <= 0 || count > 100) ? 20 : count;
            offset = Math.max(0, offset);

            TListRoleResult result = ClientHolder.get().getRole(name, count, offset);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, get role");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                _Logger.error("get role failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("items", result.getValue());
            data.put("count", count);
            data.put("offset", offset);
            ok(resp, data);
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("get role failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.ROLE_WRITE)) return;
            Map<String, Object> body = jsonBody(req);
            String name = param(req, body, "name");
            if (name == null || name.isEmpty()) {
                _Logger.info("Bad request name");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên quyền không được để trống");
                return;
            }
            if (name.length() > 100) {
                _Logger.info("Bad request name length " + name.length());
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên quyền tối đa 100 ký tự");
                return;
            }

            TRole role = new TRole();
            role.setName(name);
            TRoleResult result = ClientHolder.get().createRole(role);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, create role");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict name role (create role)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên quyền đã tồn tại");
                    return;
                }
                _Logger.error("create role failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (Exception e) {
            _Logger.error("create role failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.ROLE_WRITE)) return;
            Integer roleId = parseIdFromRequest(req);
            if (roleId == null || roleId <= 0) {
                _Logger.info("Bad request roleId " + roleId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            Map<String, Object> body = jsonBody(req);
            String name = param(req, body, "name");
            if (name == null || name.trim().isEmpty()) {
                _Logger.info("Bad request name");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên quyền không được để trống");
                return;
            }
            if (name.length() > 100) {
                _Logger.info("Bad request name length " + name.length());
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên quyền tối đa 100 ký tự");
                return;
            }

            TRole role = new TRole();
            role.setRoleId(roleId);
            role.setName(name);
            TRoleResult result = ClientHolder.get().updateRole(role);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, update role");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, update role");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy quyền để cập nhật");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict name role (update role)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên quyền đã tồn tại");
                    return;
                }
                _Logger.error("update role failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (Exception e) {
            _Logger.error("update role failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.ROLE_WRITE)) return;
            Integer roleId = parseIdFromRequest(req);
            if (roleId == null || roleId <= 0) {
                _Logger.info("Bad request roleId " + roleId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            TRoleResult result = ClientHolder.get().deleteRole(roleId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete role");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete role");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy quyền để xóa");
                    return;
                }
                _Logger.error("delete role failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, "Xóa dữ liệu thành công");
        } catch (Exception e) {
            _Logger.error("delete role failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}