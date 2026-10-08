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
import thrift.TListVoucherResult;
import thrift.TVoucher;
import thrift.TVoucherResult;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class VoucherServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Integer voucherId = parseIdFromRequest(req);
            if (voucherId == null || voucherId < 0) {
                _Logger.info("Bad request voucherId " + voucherId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            if (voucherId == 0) {
                String title = param(req, null, "title");
                String code  = param(req, null, "code");
                String countFromParam = param(req, null, "count");
                String offsetFromParam = param(req, null, "offset");
                int count = Integer.parseInt((countFromParam == null || countFromParam.trim().isEmpty()) ? "20" : countFromParam);
                int offset = Integer.parseInt((offsetFromParam == null || offsetFromParam.trim().isEmpty()) ? "0" : offsetFromParam);
                count = (count <= 0 || count > 100) ? 20 : count;
                offset = Math.max(0, offset);
                TListVoucherResult result = ClientHolder.get().getVoucher(title, count, offset, code);
                if (Err.isFail(result.getError())) {
                    if (Err.isNetworkError(result.getError())) {
                        _Logger.error("Network error, get voucher");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    }
                    _Logger.error("get voucher failed");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                    return;
                }

                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("items", result.getValue());
                data.put("count", count);
                data.put("offset", offset);
                ok(resp, data);
            }
            else
            {
                TVoucherResult result = ClientHolder.get().getVoucherById(voucherId);
                if (Err.isFail(result.getError())) {
                    if (Err.isNetworkError(result.getError())) {
                        _Logger.error("Network error, get voucher");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    }
                    if (Err.isNotFound(result.getError())) {
                        _Logger.info("Notfound, get voucher");
                        fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy voucher");
                        return;
                    }
                    _Logger.error("get voucher failed");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                    return;
                }
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("voucherId", result.getValue().getVoucherId());
                data.put("title", result.getValue().getTitle());
                data.put("code", result.getValue().getCode());
                data.put("imageUrl", result.getValue().getImageUrl());
                data.put("body", result.getValue().getBody());
                data.put("discountPercent", result.getValue().getDiscountPercent());
                data.put("maxDiscount", result.getValue().getMaxDiscount());
                data.put("startDate", result.getValue().getStartDate());
                data.put("endDate", result.getValue().getEndDate());
                ok(resp, data);
            }
        } catch (Exception e) {
            _Logger.error("get voucher failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.VOUCHER_WRITE)) return;
            Map<String, Object> body = jsonBody(req);
            String title = param(req, body, "title");
            String code = param(req, body, "code");
            String imageUrl = param(req, body, "imageUrl");
            String publicId = param(req, body, "publicId");
            String bodyFromParam = param(req, body, "body");
            String discountPercentFromParam = param(req, body, "discountPercent");
            String maxDiscountFromParam = param(req, body, "maxDiscount");
            String startDateFromParam = param(req, body, "startDate");
            String endDateFromParam = param(req, body, "endDate");

            if (title == null || title.trim().isEmpty()) {
                _Logger.info("Bad request title");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tiêu đề voucher không được để trống");
                return;
            }
            if (code == null || code.trim().isEmpty()) {
                _Logger.info("Bad request code");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mã voucher không được để trống");
                return;
            }
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                _Logger.info("Bad request imageUrl");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ảnh voucher không được để trống");
                return;
            }
            if (publicId == null || publicId.trim().isEmpty()) {
                _Logger.info("Bad request publicId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "PublicId ảnh không được để trống");
                return;
            }
            if (bodyFromParam == null || bodyFromParam.trim().isEmpty()) {
                _Logger.info("Bad request body");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Nội dung voucher không được để trống");
                return;
            }
            if (discountPercentFromParam == null || discountPercentFromParam.trim().isEmpty()) {
                _Logger.info("Bad request discountPercent");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phần trăm giảm giá không được để trống");
                return;
            }
            if (maxDiscountFromParam == null || maxDiscountFromParam.trim().isEmpty()) {
                _Logger.info("Bad request maxDiscount");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Số tiền giảm tối đa không được để trống");
                return;
            }
            if (startDateFromParam == null || startDateFromParam.trim().isEmpty()) {
                _Logger.info("Bad request startDate");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ngày bắt đầu không được để trống");
                return;
            }
            if (endDateFromParam == null || endDateFromParam.trim().isEmpty()) {
                _Logger.info("Bad request endDate");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ngày kết thúc không được để trống");
                return;
            }

            int discountPercent = Integer.parseInt(discountPercentFromParam);
            long maxDiscount = Long.parseLong(maxDiscountFromParam);
            long startDate = Long.parseLong(startDateFromParam);
            long endDate = Long.parseLong(endDateFromParam);

            if (discountPercent < 0 || discountPercent > 100) {
                _Logger.info("Bad request discountPercent : " + discountPercent);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phần trăm giảm giá phải trong khoảng 0-100");
                return;
            }
            if (maxDiscount < 0) {
                _Logger.info("Bad request maxDiscount : " + maxDiscount);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Số tiền giảm tối đa không hợp lệ");
                return;
            }
            if (startDate <= 0) {
                _Logger.info("Bad request startDate : " + startDate);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ngày bắt đầu không hợp lệ");
                return;
            }
            if (endDate <= 0 || endDate < startDate) {
                _Logger.info("Bad request endDate : " + endDate);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ngày kết thúc không hợp lệ");
                return;
            }

            TVoucher voucher = new TVoucher();
            voucher.setCreatedAt(System.currentTimeMillis());
            voucher.setTitle(title);
            voucher.setCode(code);
            voucher.setBody(bodyFromParam);
            voucher.setDiscountPercent(discountPercent);
            voucher.setMaxDiscount(maxDiscount);
            voucher.setImageUrl(imageUrl);
            voucher.setPublicId(publicId);
            voucher.setStartDate(startDate);
            voucher.setEndDate(endDate);
            TVoucherResult result = ClientHolder.get().createVoucher(voucher);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, create voucher");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict code voucher (create voucher)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Mã voucher đã tồn tại");
                    return;
                }
                _Logger.error("create voucher failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("create voucher failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.VOUCHER_WRITE)) return;
            Integer voucherId = parseIdFromRequest(req);
            if (voucherId == null || voucherId <= 0) {
                _Logger.info("Bad request voucherId " + voucherId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            Map<String, Object> body = jsonBody(req);
            String title = param(req, body, "title");
            String code = param(req, body, "code");
            String imageUrl = param(req, body, "imageUrl");
            String publicId = param(req, body, "publicId");
            String bodyFromParam = param(req, body, "body");
            String discountPercentFromParam = param(req, body, "discountPercent");
            String maxDiscountFromParam = param(req, body, "maxDiscount");
            String startDateFromParam = param(req, body, "startDate");
            String endDateFromParam = param(req, body, "endDate");

            if (title == null || title.trim().isEmpty()) {
                _Logger.info("Bad request title");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tiêu đề voucher không được để trống");
                return;
            }
            if (code == null || code.trim().isEmpty()) {
                _Logger.info("Bad request code");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mã voucher không được để trống");
                return;
            }
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                _Logger.info("Bad request imageUrl");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ảnh voucher không được để trống");
                return;
            }
            if (publicId == null || publicId.trim().isEmpty()) {
                _Logger.info("Bad request publicId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "PublicId ảnh không được để trống");
                return;
            }
            if (bodyFromParam == null || bodyFromParam.trim().isEmpty()) {
                _Logger.info("Bad request body");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Nội dung voucher không được để trống");
                return;
            }
            if (discountPercentFromParam == null || discountPercentFromParam.trim().isEmpty()) {
                _Logger.info("Bad request discountPercent");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phần trăm giảm giá không được để trống");
                return;
            }
            if (maxDiscountFromParam == null || maxDiscountFromParam.trim().isEmpty()) {
                _Logger.info("Bad request maxDiscount");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Số tiền giảm tối đa không được để trống");
                return;
            }
            if (startDateFromParam == null || startDateFromParam.trim().isEmpty()) {
                _Logger.info("Bad request startDate");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ngày bắt đầu không được để trống");
                return;
            }
            if (endDateFromParam == null || endDateFromParam.trim().isEmpty()) {
                _Logger.info("Bad request endDate");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ngày kết thúc không được để trống");
                return;
            }

            int discountPercent = Integer.parseInt(discountPercentFromParam);
            long maxDiscount = Long.parseLong(maxDiscountFromParam);
            long startDate = Long.parseLong(startDateFromParam);
            long endDate = Long.parseLong(endDateFromParam);

            if (discountPercent < 0 || discountPercent > 100) {
                _Logger.info("Bad request discountPercent : " + discountPercent);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phần trăm giảm giá phải trong khoảng 0-100");
                return;
            }
            if (maxDiscount < 0) {
                _Logger.info("Bad request maxDiscount : " + maxDiscount);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Số tiền giảm tối đa không hợp lệ");
                return;
            }
            if (startDate <= 0) {
                _Logger.info("Bad request startDate : " + startDate);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ngày bắt đầu không hợp lệ");
                return;
            }
            if (endDate <= 0 || endDate < startDate) {
                _Logger.info("Bad request endDate : " + endDate);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ngày kết thúc không hợp lệ");
                return;
            }

            TVoucher voucher = new TVoucher();
            voucher.setVoucherId(voucherId);
            voucher.setTitle(title);
            voucher.setCode(code);
            voucher.setBody(bodyFromParam);
            voucher.setDiscountPercent(discountPercent);
            voucher.setMaxDiscount(maxDiscount);
            voucher.setImageUrl(imageUrl);
            voucher.setPublicId(publicId);
            voucher.setStartDate(startDate);
            voucher.setEndDate(endDate);
            TVoucherResult result = ClientHolder.get().updateVoucher(voucher);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, update voucher");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, update voucher");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy voucher để cập nhật");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict code voucher (update voucher)");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Mã voucher đã tồn tại");
                    return;
                }
                _Logger.error("updated voucher failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("Update voucher failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        try {
            if (!hasRole(req, resp, Role.VOUCHER_WRITE)) return;
            Integer voucherId = parseIdFromRequest(req);
            if (voucherId == null || voucherId <= 0) {
                _Logger.info("Bad request voucherId " + voucherId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            TVoucherResult result = ClientHolder.get().deleteVoucher(voucherId);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete voucher");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete voucher");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy ưu đãi để xóa");
                    return;
                }
                _Logger.error("Deleted voucher failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, new TVoucherResult());
        } catch (Exception e) {
            _Logger.error("Delete voucher failed");
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

}
