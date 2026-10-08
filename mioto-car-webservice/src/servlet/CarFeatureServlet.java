/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import thrift.TCarFeature;
import thrift.TListCarFeatureViewResult;
import thrift.TUser;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class CarFeatureServlet extends AuthServlet {

    // POST body: {"carId": 7, "features": [1, 2, 3]}
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            TUser user = getUserFromRequest(req);
            Map<String, Object> body = jsonBody(req);
            String carIdFromParam = param(req, body, "carId");
            if (carIdFromParam == null) {
                _Logger.info("Bad request carId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Xe không được để trống");
                return;
            }
            int carId = Integer.parseInt(carIdFromParam);
            if (carId <= 0) {
                _Logger.info("Bad request carId : " + carId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Xe không hợp lệ");
                return;
            }

            Object rawFeatures = body == null ? null : body.get("featureIds");
            if (rawFeatures == null || !(rawFeatures instanceof List) || ((List<?>) rawFeatures).isEmpty() || ((List<?>) rawFeatures).size() > 100) {
                _Logger.info("Bad request features");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách tính năng không hợp lệ");
                return;
            }
            List<TCarFeature> features = new ArrayList<TCarFeature>();
            for (Object o : (List<?>) rawFeatures) {
                if (!(o instanceof Number) || ((Number) o).intValue() <= 0) {
                    _Logger.info("Bad request feature item");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tính năng không hợp lệ");
                    return;
                }
                TCarFeature feature = new TCarFeature();
                feature.setCarId(carId);             // model lấy carId từ phần tử đầu
                feature.setFeatureId(((Number) o).intValue());
                features.add(feature);
            }

            TListCarFeatureViewResult result = ClientHolder.get().createCarFeatures(features, (int) user.getUserId());
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, create car features");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (result.getError() == Err.FORBIDDEN) {
                    _Logger.info("Forbidden, create car features, carId=" + carId);
                    fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN, "Bạn không phải chủ của xe này");
                    return;
                }
                _Logger.error("create car features failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("items", result.getValue());
            ok(resp, data);
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("create car features failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    // DELETE body: {"carId": 7, "carFeatureIds": [1, 2, 3]}  (id dòng CarFeatures = TCarFeatureView.id)
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            
            TUser user = getUserFromRequest(req);
            Map<String, Object> body = jsonBody(req);
            String carIdFromParam = param(req, body, "carId");
            if (carIdFromParam == null) {
                _Logger.info("Bad request carId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Xe không được để trống");
                return;
            }
            int carId = Integer.parseInt(carIdFromParam);
            if (carId <= 0) {
                _Logger.info("Bad request carId : " + carId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Xe không hợp lệ");
                return;
            }

            Object rawIds = body == null ? null : body.get("carFeatureIds");
            if (rawIds == null || !(rawIds instanceof List) || ((List<?>) rawIds).isEmpty() || ((List<?>) rawIds).size() > 100) {
                _Logger.info("Bad request carFeatureIds");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách tính năng cần xóa không hợp lệ");
                return;
            }
            List<Long> carFeatureIds = new ArrayList<Long>();
            for (Object o : (List<?>) rawIds) {
                if (!(o instanceof Number) || ((Number) o).longValue() <= 0) {
                    _Logger.info("Bad request carFeatureId item");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id tính năng không hợp lệ");
                    return;
                }
                carFeatureIds.add(((Number) o).longValue());
            }

            TListCarFeatureViewResult result = ClientHolder.get().deleteCarFeatures(carId, carFeatureIds, (int) user.getUserId());
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete car features");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (result.getError() == Err.FORBIDDEN) {
                    _Logger.info("Forbidden, delete car features, carId=" + carId);
                    fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN, "Bạn không phải chủ của xe này");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete car features");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy tính năng để xóa");
                    return;
                }
                _Logger.error("delete car features failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, "Xóa dữ liệu thành công");
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("delete car features failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}