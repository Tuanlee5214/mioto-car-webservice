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
import thrift.TFeature;
import thrift.TFeatureResult;
import thrift.TListFeatureResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class FeatureServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Integer featureId = parseIdFromRequest(req);
            if (featureId == null || featureId < 0) {
                _Logger.info("Bad request feature id :");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            if (featureId == 0) {
                String nameFeature = param(req, null, "nameFeature");
                String countFromParam = param(req, null, "count");
                String offsetFromParam = param(req, null, "offset");
                int count = Integer.parseInt((countFromParam == null || countFromParam.trim().isEmpty()) ? "20" : countFromParam);
                int offset = Integer.parseInt((offsetFromParam == null || offsetFromParam.trim().isEmpty()) ? "0" : offsetFromParam);
                count = (count <= 0 || count > 100) ? 20 : count;
                offset = Math.max(0, offset);
                TListFeatureResult result = ClientHolder.get().getFeature(nameFeature, count, offset);
                if (Err.isFail(result.getError())) {
                    if (Err.isNetworkError(result.getError())) {
                        _Logger.error("Network error(get all features)");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    }
                    _Logger.error("get all feature value failed");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                    return;
                }
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("items", result.getValue());
                data.put("count", count);
                data.put("offset", offset);
                ok(resp, data);
            } else {
                TFeatureResult ret = ClientHolder.get().getFeatureById(featureId);
                if (Err.isFail(ret.getError())) {
                    if (Err.isNetworkError(ret.getError())) {
                        _Logger.error("Network error(get feature by id)");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    } else if (Err.isNotFound(ret.getError())) {
                        _Logger.info("Notfound error (get feature by id)");
                        fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.FAIL, "Không tìm thấy tính năng này");
                        return;
                    } else {
                        _Logger.error("get feature value failed");
                        fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                        return;
                    }
                }
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("featureId", ret.getValue().getFeatureId());
                data.put("nameFeature", ret.getValue().getNameFeature());
                data.put("createdAt", ret.getValue().getCreatedAt());
                data.put("updatedAt", ret.getValue().getUpdatedAt());
                ok(resp, data);
            }
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("feature get value failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.FEATURE_WRITE)) return;
            Map<String, Object> body = jsonBody(req);
            String nameFeature = param(req, body, "nameFeature");
            if (nameFeature == null || nameFeature.trim().isEmpty()) {
                _Logger.info("Bad request nameFeature");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên tính năng không được để trống");
                return;
            }
            TFeature feature = new TFeature();
            feature.setNameFeature(nameFeature.trim());
            feature.setCreatedAt(System.currentTimeMillis());
            feature.setUpdatedAt(System.currentTimeMillis());
            TFeatureResult result = ClientHolder.get().createFeature(feature);

            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error(create feature)");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict nameDistrict(create feature)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên tính năng đã tồn tại");
                    return;
                }
                _Logger.error("Create feature failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }

            ok(resp, result.getValue());
        } catch (Exception e) {
            _Logger.error("feature create failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.FEATURE_WRITE)) return;
            Integer featureId = parseIdFromRequest(req);
            if (featureId == null || featureId <= 0) {
                _Logger.info("Bad request feature id : " + featureId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            Map<String, Object> body = jsonBody(req);
            String nameFeature = param(req, body, "nameFeature");
            if (nameFeature == null || nameFeature.trim().isEmpty()) {
                _Logger.info("Bad request nameFeature");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên tính năng không được để trống");
                return;
            }

            TFeature feature = new TFeature();
            feature.setFeatureId(featureId);
            feature.setNameFeature(nameFeature.trim());
            feature.setUpdatedAt(System.currentTimeMillis());
            TFeatureResult result = ClientHolder.get().updateFeature(feature);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, update feature");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict name feature (update feature)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên tính năng đã tồn tại");
                    return;
                }
                if(Err.isNotFound(result.getError()))
                {
                    _Logger.info("Notfound, update feature");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.BAD_REQUEST, "Không tìm thấy tính năng để cập nhật");
                    return;
                }
                _Logger.error("updated feature failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (Exception e) {
            _Logger.error("updated feature failed");
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.FEATURE_WRITE)) return;
            Integer featureId = parseIdFromRequest(req);
            if (featureId == null || featureId <= 0) {
                _Logger.info("Bad request feature id : " + featureId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            TFeatureResult result = ClientHolder.get().deleteFeature(featureId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete feature");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete feature");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.BAD_REQUEST, "Không tìm thấy tính năng để xóa");
                    return;
                }
                _Logger.error("Deleted feature failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, "Xoá dữ liệu thành công");
        } catch (Exception e) {
            _Logger.error("feature delete failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}
