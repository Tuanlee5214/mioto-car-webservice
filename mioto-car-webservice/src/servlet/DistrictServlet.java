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
import thrift.TDistrict;
import thrift.TDistrictResult;
import thrift.TListDistrictResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class DistrictServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Integer provinceId = parseIdFromRequest(req);
            if (provinceId == null || provinceId <= 0) {
                _Logger.info("Bad request province id : ");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            String countFromParam = param(req, null, "count");
            String offsetFromParam = param(req, null, "offset");
            int count = Integer.parseInt((countFromParam == null || countFromParam.trim().isEmpty()) ? "20" : countFromParam);
            int offset = Integer.parseInt((offsetFromParam == null || offsetFromParam.trim().isEmpty()) ? "0" : offsetFromParam);
            count = (count <= 0 || count > 100) ? 20 : count;
            offset = Math.max(0, offset);
            TListDistrictResult result = ClientHolder.get().getDistrict(provinceId, count, offset);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error(get district)");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                _Logger.error("get district failed");
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
            return;
        } catch (Exception e) {
            _Logger.error("district get failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if(!hasRole(req, resp, Role.DISTRICT_WRITE)) return;
            
            Map<String, Object> body = jsonBody(req);
            String nameDistrict = param(req, body, "nameDistrict");
            String provinceIdFromParam = param(req, body, "provinceId");
            if (nameDistrict == null || nameDistrict.trim().isEmpty()) {
                _Logger.info("Bad request name district");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên quận/huyện không được để trống");
                return;
            }
            if (provinceIdFromParam == null || provinceIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request provinceId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mã tỉnh/thành phố không được để trống");
                return;
            }
            long provinceId = Integer.parseInt(provinceIdFromParam);
            TDistrict district = new TDistrict();
            district.setDistrictName(nameDistrict.trim());
            district.setProvinceId((int) provinceId);
            TDistrictResult result = ClientHolder.get().createDistrict(district);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error(create district)");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict nameDistrict(create district)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên quận/huyện đã tồn tại");
                    return;
                }
                _Logger.error("Create district failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.value);

        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("district create failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.DISTRICT_WRITE)) return;

            Integer districtId = parseIdFromRequest(req);
            if (districtId == null || districtId <= 0) {
                _Logger.info("Bad request district id : " + districtId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            Map<String, Object> body = jsonBody(req);
            String nameDistrict = param(req, body, "nameDistrict");
            if (nameDistrict == null || nameDistrict.trim().isEmpty()) {
                _Logger.info("Bad request name district");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên quận/huyện không được bỏ trống");
                return;
            }

            TDistrict district = new TDistrict();
            district.setDistrictId(districtId);
            district.setDistrictName(nameDistrict.trim());
            TDistrictResult result = ClientHolder.get().updateDistrict(district);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, update district");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict name district (update district)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên quận/huyện đã tồn tại");
                    return;
                }
                if(Err.isNotFound(result.getError()))
                {
                    _Logger.info("Notfound, update district");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy quận/huyện để cập nhật");
                    return;
                }
                _Logger.error("updated district failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (Exception e) {
            _Logger.error("updated district failed");
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.DISTRICT_WRITE)) return;

            Integer districtId = parseIdFromRequest(req);
            if (districtId == null || districtId <= 0) {
                _Logger.info("Bad request district id : " + districtId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            TDistrictResult result = ClientHolder.get().deleteDistrict(districtId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete district");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete district");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.BAD_REQUEST, "Không tìm thấy quận/huyện để xóa");
                    return;
                }
                _Logger.error("Deleted district failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, "Xóa dữ liệu thành công");

        } catch (Exception e) {
            _Logger.error("district delete failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

}
