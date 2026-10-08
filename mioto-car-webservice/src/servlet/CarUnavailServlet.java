/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import thrift.TCarUnavails;
import thrift.TCarUnavailsResult;
import thrift.TListCarUnavailsResult;
import thrift.TUser;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class CarUnavailServlet extends AuthServlet {

    // GET /api/car-unavails?carId=7
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String carIdFromParam = param(req, null, "carId");
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
            TListCarUnavailsResult result = ClientHolder.get().getListCarUnavails(carId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, get car unavails");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                _Logger.error("get car unavails failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("items", result.getValue() != null ? result.getValue() : new ArrayList<TCarUnavails>());
            ok(resp, data);
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("get car unavails failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    // POST body: {"carId": 7, "startTime": 1791594000000, "endTime": 1791810000000}
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            TUser user = getUserFromRequest(req);
            Map<String, Object> body = jsonBody(req);
            String carIdFromParam = param(req, body, "carId");
            String startTimeFromParam = param(req, body, "startTime");
            String endTimeFromParam = param(req, body, "endTime");
            if (carIdFromParam == null || carIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request carId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Xe không được để trống");
                return;
            }
            if (startTimeFromParam == null || carIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request startTime");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thời gian bắt đầu không được để trống");
                return;
            }
            if (endTimeFromParam == null || carIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request endTime");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thời gian kết thúc không được để trống");
                return;
            }
            int carId = Integer.parseInt(carIdFromParam);
            long startTime = Long.parseLong(startTimeFromParam);
            long endTime = Long.parseLong(endTimeFromParam);
            if (carId <= 0) {
                _Logger.info("Bad request carId : " + carId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Xe không hợp lệ");
                return;
            }
            if (startTime <= 0) {
                _Logger.info("Bad request startTime : " + startTime);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thời gian bắt đầu không hợp lệ");
                return;
            }
            if (endTime <= startTime) {
                _Logger.info("Bad request endTime : " + endTime);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu");
                return;
            }

            long now = System.currentTimeMillis();
            TCarUnavails unavail = new TCarUnavails();
            unavail.setCarId(carId);
            unavail.setStartTime(startTime);
            unavail.setEndTime(endTime);
            unavail.setCreatedAt(now);
            unavail.setUpdatedAt(now);

            TCarUnavailsResult result = ClientHolder.get().createCarUnavails(unavail, (int) user.getUserId());
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, create car unavail");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (result.getError() == Err.FORBIDDEN) {
                    _Logger.info("Forbidden, create car unavail, carId=" + carId);
                    fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN, "Bạn không phải chủ của xe này");
                    return;
                }
                _Logger.error("create car unavail failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("create car unavail failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    // DELETE body: {"carId": 7, "carUnavailId": 3}
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            TUser user = getUserFromRequest(req);
            Map<String, Object> body = jsonBody(req);
            String carIdFromParam = param(req, body, "carId");
            String carUnavailIdFromParam = param(req, body, "carUnavailId");
            if (carIdFromParam == null || carIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request carId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Xe không được để trống");
                return;
            }
            if (carUnavailIdFromParam == null || carUnavailIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request carUnavailId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Lịch bận không được để trống");
                return;
            }
            int carId = Integer.parseInt(carIdFromParam);
            long carUnavailId = Long.parseLong(carUnavailIdFromParam);
            if (carId <= 0) {
                _Logger.info("Bad request carId : " + carId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Xe không hợp lệ");
                return;
            }
            if (carUnavailId <= 0) {
                _Logger.info("Bad request carUnavailId : " + carUnavailId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Lịch bận không hợp lệ");
                return;
            }

            TCarUnavailsResult result = ClientHolder.get().deleteCarUnavails(carId, carUnavailId, (int) user.getUserId());
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete car unavail");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (result.getError() == Err.FORBIDDEN) {
                    _Logger.info("Forbidden, delete car unavail, carId=" + carId);
                    fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN, "Bạn không phải chủ của xe này");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete car unavail");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy lịch bận để xóa");
                    return;
                }
                _Logger.error("delete car unavail failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, "Xóa dữ liệu thành công");
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("delete car unavail failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}