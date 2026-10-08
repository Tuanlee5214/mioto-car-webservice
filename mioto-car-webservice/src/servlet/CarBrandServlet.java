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
import thrift.TCarBrand;
import thrift.TCarBrandResult;
import thrift.TListCarBrandResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class CarBrandServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            
            Integer carBrandId = parseIdFromRequest(req);
            if (carBrandId == null || carBrandId < 0) {
                _Logger.info("Bad request car brand id : " + carBrandId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            
            if (carBrandId == 0) {
                String nameBrand = param(req, null, "nameBrand");
                String countFromParam = param(req, null, "count");
                String offsetFromParam = param(req, null, "offset");
                int count = Integer.parseInt((countFromParam == null || countFromParam.trim().isEmpty()) ? "20" : countFromParam);
                int offset = Integer.parseInt((offsetFromParam == null || offsetFromParam.trim().isEmpty()) ? "0" : offsetFromParam);
                count = (count <= 0 || count > 100) ? 20 : count;
                offset = Math.max(0, offset);
                TListCarBrandResult ret = ClientHolder.get().getCarBrand(nameBrand, count, offset);
                if (Err.isFail(ret.getError())) {
                    if (Err.isNetworkError(ret.getError())) {
                        _Logger.error("Network error(get all car brand)");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    } else {
                        _Logger.error("get all car brand value failed");
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
                
                TCarBrandResult ret = ClientHolder.get().getCarBrandById(carBrandId);
                if (Err.isFail(ret.getError())) {
                    if (Err.isNetworkError(ret.getError())) {
                        _Logger.error("Network error(get car brand by id)");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    } else if (Err.isNotFound(ret.getError())) {
                        _Logger.info("Notfound error (get car brand by id)");
                        fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy hãng xe này");
                        return;
                    } else {
                        _Logger.error("get car brand value failed");
                        fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                        return;
                    }

                }

                TCarBrand carBrand = ret.getValue();
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("carBrandId", carBrand.getCarBrandId());
                data.put("nameBrand", carBrand.getNameBrand());
                data.put("createdAt", carBrand.getCreatedAt());
                data.put("updatedAt", carBrand.getUpdatedAt());
                ok(resp, data);
            }
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("car brand get value failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if(!hasRole(req, resp, Role.CAR_BRAND_WRITE)) return;
            
            Map<String, Object> body = jsonBody(req);
            String nameBrand = param(req, body, "nameBrand");
            if (nameBrand == null || nameBrand.trim().isEmpty()) {
                _Logger.info("Bad request name brand");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên hãng xe không được để trống");
                return;
            }

            TCarBrand carBrand = new TCarBrand();
            carBrand.setNameBrand(nameBrand.trim());
            carBrand.setUpdatedAt(System.currentTimeMillis());
            carBrand.setCreatedAt(System.currentTimeMillis());
            TCarBrandResult result = ClientHolder.get().createCarBrand(carBrand);
            //check trung sau nay nhe duplicate 
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error(create CarBrand)");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict nameBrand(create carBrand)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên hãng xe đã tồn tại");
                    return;
                }
                _Logger.error("Create car brand failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.value);
        } catch (Exception e) {
            _Logger.error("car brand create failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if(!hasRole(req, resp, Role.CAR_BRAND_WRITE)) return;

            Integer carBrandId = parseIdFromRequest(req);
            if (carBrandId == null || carBrandId <= 0) {
                _Logger.info("Bad request car brand id : " + carBrandId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            Map<String, Object> body = jsonBody(req);
            String nameBrand = param(req, body, "nameBrand");
            if (nameBrand == null || nameBrand.trim().isEmpty()) {
                _Logger.info("Bad request name brand");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên hãng xe không được để trống");
                return;
            }

            TCarBrand carBrand = new TCarBrand();
            carBrand.setCarBrandId(carBrandId);
            carBrand.setNameBrand(nameBrand.trim());
            carBrand.setUpdatedAt(System.currentTimeMillis());
            TCarBrandResult result = ClientHolder.get().updateCarBrand(carBrand);

            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error(update carBrand)");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict nameBrand(update carBrand)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên hãng xe đã tồn tại");
                    return;
                }
                if(Err.isNotFound(result.getError()))
                {
                    _Logger.info("Notfound update car brand");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.FAIL, "Không tìm thấy hãng xe để cập nhật");
                    return;
                }
                _Logger.error("Update carbrand failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }

            ok(resp, result.value);

        } catch (Exception e) {
            _Logger.error("car brand update failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if(!hasRole(req, resp, Role.CAR_BRAND_WRITE)) return;

            Integer carBrandId = parseIdFromRequest(req);
            if (carBrandId == null || carBrandId <= 0) {
                _Logger.info("Bad request car brand id : " + carBrandId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            TCarBrandResult result = ClientHolder.get().deleteCarBrand(carBrandId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete car brand");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound delete car brand");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.FAIL, "Không tìm thấy hãng xe để xóa");
                    return;
                }
                _Logger.error("Deleted car brand failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, "Xóa dữ liệu thành công");
        } catch (Exception e) {
            _Logger.error("car brand delete failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

}
