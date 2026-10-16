/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import role.Role;
import static servlet.BaseServlet._Logger;
import thrift.TFeePolicy;
import thrift.TFeePolicyResult;
import thrift.TListFeePolicyResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class FeePolicyServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Integer feePolicyId = parseIdFromRequest(req);
            if (feePolicyId == null || feePolicyId < 0) {
                _Logger.info("Bad request feature id : ");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            if (feePolicyId == 0) {
                String nameFeePolicy = param(req, null, "nameFeePolicy");
                String countFromParam = param(req, null, "count");
                String offsetFromParam = param(req, null, "offset");
                int count = Integer.parseInt((countFromParam == null || countFromParam.trim().isEmpty()) ? "20" : countFromParam);
                int offset = Integer.parseInt((offsetFromParam == null || offsetFromParam.trim().isEmpty()) ? "0" : offsetFromParam);
                count = (count <= 0 || count > 100) ? 20 : count;
                offset = Math.max(0, offset);
                TListFeePolicyResult result = ClientHolder.get().getFeePolicy(nameFeePolicy, count, offset);
                if(Err.isFail(result.getError()))
                {
                    if (Err.isNetworkError(result.getError())) {
                        _Logger.error("Network error(get all feepolicy)");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    }
                    _Logger.error("get all fee policy value failed");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                    return;
                }
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("items", result.getValue());
                data.put("count", count);
                data.put("offset", offset);
                ok(resp, data);
            } else {
                TFeePolicyResult result = ClientHolder.get().getFeePolicyById(feePolicyId);
                if(Err.isFail(result.getError()))
                {
                    if (Err.isNetworkError(result.getError())) {
                        _Logger.error("Network error(get feature by id)");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    } else if (Err.isNotFound(result.getError())) {
                        _Logger.info("Notfound error (get feature by id)");
                        fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.FAIL, "Không tìm thấy chính sách phí này");
                        return;
                    } else {
                        _Logger.error("get feature value failed");
                        fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                        return;
                    }
                }
                
                Map<String,  Object> data = new LinkedHashMap<String, Object>();
                data.put("feePolicyId", result.getValue().getFeePolicyId());
                data.put("nameFeePolicy", result.getValue().getName());
                data.put("percentFee", result.getValue().getPercentFee());
                data.put("isActive", result.getValue().isIsActive());
                ok(resp, data);
            }
        } catch (Exception e) {
            _Logger.error("fee policy get value failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.FEE_POLICY_WRITE)) return;
            Map<String, Object> body = jsonBody(req);
            String nameFeePolicy = param(req, body, "nameFeePolicy");
            String percentFeeFromParam = param(req, body, "percentFee");
            String isActiveFromParam = param(req, body, "isActive");
            if (nameFeePolicy == null || nameFeePolicy.trim().isEmpty()) {
                _Logger.info("Bad request name feepolicy");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên chính sách phí không được để trống");
                return;
            }
            if (percentFeeFromParam == null || percentFeeFromParam.trim().isEmpty()) {
                _Logger.info("Bad request percentFee");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phần trăm chính sách phí không được để trống");
                return;
            }
            BigDecimal percentFee = new BigDecimal(percentFeeFromParam.replace(",", "."));
            if (percentFee.compareTo(BigDecimal.ZERO) < 0 || percentFee.compareTo(new BigDecimal("100")) > 0) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phần trăm phí phải trong khoảng 0-100");
                return;
            }
            if (isActiveFromParam == null || isActiveFromParam.trim().isEmpty()) {
                _Logger.info("Bad request isActiveFromParam");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Trạng thái chính sách phí không được để trống");
                return;
            }
            boolean isActive;
            if (isActiveFromParam.equalsIgnoreCase("true")) {
                isActive = true;
            } else if (isActiveFromParam.equalsIgnoreCase("false")) {
                isActive = false;
            } else {
                _Logger.info("Bad request isActiveFromParam");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Trạng thái chính sách phí có giá trị khác true/false");
                return;
            }
            TFeePolicy feePolicy = new TFeePolicy();
            feePolicy.setName(nameFeePolicy);
            feePolicy.setIsActive(isActive);
            feePolicy.setPercentFee(percentFee.toPlainString());
            TFeePolicyResult result = ClientHolder.get().createFeePolicy(feePolicy);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, create feePolicy");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict name feePolicy (create feePolicy)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên chính sách phí đã tồn tại");
                    return;
                }
                _Logger.error("create fee policy failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("fee policy create failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.FEE_POLICY_WRITE)) return;
            Integer feePolicyId = parseIdFromRequest(req);
            if (feePolicyId == null || feePolicyId <= 0) {
                _Logger.info("Bad request feature id : ");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            Map<String, Object> body = jsonBody(req);
            String nameFeePolicy = param(req, body, "nameFeePolicy");
            String percentFeeFromParam = param(req, body, "percentFee");
            String isActiveFromParam = param(req, body, "isActive");
            if (nameFeePolicy == null || nameFeePolicy.trim().isEmpty()) {
                _Logger.info("Bad request name feepolicy");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên chính sách phí không được để trống");
                return;
            }
            if (percentFeeFromParam == null || percentFeeFromParam.trim().isEmpty()) {
                _Logger.info("Bad request percentFee");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phần trăm chính sách phí không được để trống");
                return;
            }
            BigDecimal percentFee = new BigDecimal(percentFeeFromParam.replace(",", "."));
            if (percentFee.compareTo(BigDecimal.ZERO) < 0 || percentFee.compareTo(new BigDecimal("100")) > 0) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phần trăm phí phải trong khoảng 0-100");
                return;
            }
            if (isActiveFromParam == null || isActiveFromParam.trim().isEmpty()) {
                _Logger.info("Bad request isActiveFromParam");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Trạng thái chính sách phí không được để trống");
                return;
            }
            boolean isActive;
            if (isActiveFromParam.equalsIgnoreCase("true")) {
                isActive = true;
            } else if (isActiveFromParam.equalsIgnoreCase("false")) {
                isActive = false;
            } else {
                _Logger.info("Bad request isActiveFromParam");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Trạng thái chính sách phí có giá trị khác true/false");
                return;
            }
            TFeePolicy feePolicy = new TFeePolicy();
            feePolicy.setFeePolicyId(feePolicyId);
            feePolicy.setName(nameFeePolicy);
            feePolicy.setIsActive(isActive);
            feePolicy.setPercentFee(percentFee.toPlainString());
            TFeePolicyResult result = ClientHolder.get().updateFeePolicy(feePolicy);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, update feePolicy");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict name feePolicy (update feePolicy)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Tên chính sách phí đã tồn tại");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, update feePolicy");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.BAD_REQUEST, "Không tìm thấy chính sách phí để cập nhật");
                    return;
                }
                _Logger.error("updated fee policy failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("fee policy update failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.FEE_POLICY_WRITE)) return;
            Integer feePolicyId = parseIdFromRequest(req);
            if (feePolicyId == null || feePolicyId <= 0) {
                _Logger.info("Bad request feature id : ");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            TFeePolicyResult result = ClientHolder.get().deleteFeePolicy(feePolicyId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete feePolicy");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete feePolicy");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.BAD_REQUEST, "Không tìm thấy chính sách phí để xóa");
                    return;
                }
                _Logger.error("delete fee policy failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, "Xóa dữ liệu thành công");

        } catch (Exception e) {
            _Logger.error("feepolicy delete failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

}
