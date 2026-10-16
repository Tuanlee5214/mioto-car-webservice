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
import static servlet.BaseServlet._Logger;
import thrift.TListUserResult;
import thrift.TUser;
import thrift.TUserResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class UserServlet extends AuthServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    {
        try {
            
            Integer userId = parseIdFromRequest(req);
            if (userId == null || userId < 0) {
                _Logger.info("Bad request user id : " + userId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            if (userId == 0) {
                if(!hasRole(req, resp, Role.USER_LIST_READ)) return;
                int status = -1;
                String statusFromParam = param(req, null, "status");
                if (statusFromParam != null && !statusFromParam.trim().isEmpty()) {
                    if(Integer.parseInt(statusFromParam) <= 0 || Integer.parseInt(statusFromParam) > 3)
                    {
                        _Logger.info("Bad request status : " + status);
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Giá trị trạng thái không hợp lệ");
                        return;
                    }
                    status = Integer.parseInt(statusFromParam);
                }
                String countFromParam = param(req, null, "count");
                String offsetFromParam = param(req, null, "offset");
                int count = Integer.parseInt((countFromParam == null || countFromParam.trim().isEmpty()) ? "20" : countFromParam);
                int offset = Integer.parseInt((offsetFromParam == null || offsetFromParam.trim().isEmpty()) ? "0" : offsetFromParam);
                count = (count <= 0 || count > 100) ? 20 : count;
                offset = Math.max(0, offset);
                TListUserResult ret = ClientHolder.get().getAllUser(status, count, offset);
                if (Err.isFail(ret.getError())) {
                    if (Err.isNetworkError(ret.getError())) {
                        _Logger.error("Network error(get all users)");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    } else {
                        _Logger.error("get all users value failed");
                        fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                        return;
                    }
                }
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("items", ret.getValue());
                data.put("count", count);
                data.put("offset", offset);
                ok(resp, data);
            } else {
                
            TUserResult ret = ClientHolder.get().getUser(userId);
            if(Err.isFail(ret.getError()))
            {
                if(Err.isNetworkError(ret.getError()))
                {
                    _Logger.info("Network error");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if(Err.isNotFound(ret.getError()))
                {
                    _Logger.info("Notfound error");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy người dùng này");
                    return;
                }
                _Logger.error("get user fail");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            
            TUser result = ret.getValue();
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("userId", result.getUserId());
            data.put("phone", result.getPhone());
            data.put("email", result.getEmail());
            data.put("displayName", result.getDisplayName());
            data.put("status", result.getStatus());
            ok(resp, data);
            }
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("users get value failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}
