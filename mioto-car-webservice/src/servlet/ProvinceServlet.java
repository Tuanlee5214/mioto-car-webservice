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
import thrift.TListProvinceResult;
import thrift.TProvince;
import thrift.TProvinceResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class ProvinceServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Integer provinceId = parseIdFromRequest(req);
            if (provinceId == null || provinceId < 0) {
                _Logger.info("Bad request province id : ");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            if (provinceId == 0) {
                String nameProvince = param(req, null, "nameProvince");
                String countFromParam = param(req, null, "count");
                String offsetFromParam = param(req, null, "offset");
                int count = Integer.parseInt((countFromParam == null || countFromParam.trim().isEmpty()) ? "20" : countFromParam);
                int offset = Integer.parseInt((offsetFromParam == null || offsetFromParam.trim().isEmpty()) ? "0" : offsetFromParam);
                count = (count <= 0 || count > 100) ? 20 : count;
                offset = Math.max(0, offset);
                TListProvinceResult result = ClientHolder.get().getProvince(nameProvince, count, offset);
                if (Err.isFail(result.getError())) {
                    if (Err.isNetworkError(result.getError())) {
                        _Logger.error("Network error(get province)");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    }
                    _Logger.error("get province failed");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                    return;
                }
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("items", result.getValue());
                data.put("count", count);
                data.put("offset", offset);
                ok(resp, data);
            } 
            else {
                TProvinceResult result = ClientHolder.get().getProvinceById(provinceId);
                if(Err.isFail(result.getError()))
                {
                    if (Err.isNetworkError(result.getError())) {
                        _Logger.error("Network error(get province by id)");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    } else if (Err.isNotFound(result.getError())) {
                        _Logger.info("Notfound error (get province by id)");
                        fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy tỉnh/thành phố này");
                        return;
                    } else {
                        _Logger.error("get province value failed");
                        fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                        return;
                    }
                }
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("provinceId", result.getValue().getProvinceId());
                data.put("nameProvince", result.getValue().getProvinceName());
                ok(resp, data);
            }
        }
        catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        }
        catch (Exception e) {
            _Logger.error("province get failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.PROVINCE_WRITE)) return;

            Map<String, Object> body = jsonBody(req);
            String nameProvince = param(req, body, "nameProvince");
            if (nameProvince == null || nameProvince.trim().isEmpty()) {
                _Logger.info("Bad request nameProvince");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên tỉnh/thành phố không được để trống");
                return;
            }
            TProvince province = new TProvince();
            province.setProvinceName(nameProvince);
            TProvinceResult result = ClientHolder.get().createProvince(province);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error(create province)");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict name province(create province)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên tỉnh/thành phố đã tồn tại");
                    return;
                }
                _Logger.error("Create province failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.value);

        } catch (Exception e) {
            _Logger.error("province create failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.PROVINCE_WRITE)) return;

            Integer provinceId = parseIdFromRequest(req);
            if (provinceId == null || provinceId <= 0) {
                _Logger.info("Bad request provinceId  : " + provinceId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            Map<String, Object> body = jsonBody(req);
            String nameProvince = param(req, body, "nameProvince");
            if (nameProvince == null || nameProvince.trim().isEmpty()) {
                _Logger.info("Bad request nameProvince");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên tỉnh/thành phố không được bỏ trống");
                return;
            }

            TProvince province = new TProvince();
            province.setProvinceId(provinceId);
            province.setProvinceName(nameProvince);
            TProvinceResult result = ClientHolder.get().updateProvince(province);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, update province");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict name province (update province)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên tỉnh/thành phố đã tồn tại");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, update province");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy tỉnh/thành phố để cập nhật");
                    return;
                }
                _Logger.error("updated province failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (Exception e) {
            _Logger.error("updated province failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.PROVINCE_WRITE)) return;

            Integer provinceId = parseIdFromRequest(req);
            if (provinceId == null || provinceId <= 0) {
                _Logger.info("Bad request provinceId  : " + provinceId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            TProvinceResult result = ClientHolder.get().deleteProvince(provinceId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete province");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete province");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy tỉnh/thành phố để xóa");
                    return;
                }
                _Logger.error("Deleted province failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, new TProvince());

        } catch (Exception e) {
            _Logger.error("province delete failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

}
