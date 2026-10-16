/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import imageType.ImageType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import role.Role;
import static servlet.AuthServlet.ATTR_USER;
import static servlet.BaseServlet._Logger;
import thrift.TListVoucherResult;
import thrift.TUser;
import thrift.TUserResult;
import thrift.TVoucher;
import thrift.TVoucherResult;
import util.ClientHolder;
import util.ImageStorage;

/**
 *
 * @author tuanlee
 */
public class VoucherServlet extends AuthWithoutGetServlet {


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
                String code = param(req, null, "code");
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
                List<TVoucher> vouchers = result.getValue();
                if (vouchers != null) {
                    for (TVoucher r : vouchers) {
                        r.setImageUrl(ImageStorage.buildUrl(ImageType.VOUCHER_TYPE, r.getPublicId()));
                    }
                }

                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("items", result.getValue());
                data.put("count", count);
                data.put("offset", offset);
                ok(resp, data);
            } else {
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
                data.put("publicId", result.getValue().getPublicId());
                data.put("imageUrl", ImageStorage.buildUrl(ImageType.VOUCHER_TYPE, result.getValue().getPublicId()));
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
            TUser user = getUserFromRequest(req);
            if (!hasRole(req, resp, Role.VOUCHER_WRITE)) {
                return;
            }
            Map<String, Object> body = jsonBody(req);
            String title = param(req, body, "title");
            String code = param(req, body, "code");
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
            String publicId = "";

            Object rawImages = body == null ? null : body.get("images");

            if (rawImages != null) {
                if (!(rawImages instanceof List)) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách ảnh không hợp lệ");
                    return;
                }
                int count = ((List<?>) rawImages).size();
                if (count > 1) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST,
                            "Voucher chỉ được phép có duy nhất 1 ảnh");
                    return;
                }
                for (Object o : (List<?>) rawImages) {
                    if (o instanceof Map && ((Map<?, ?>) o).get("publicId") != null) {
                        publicId = String.valueOf(((Map<?, ?>) o).get("publicId")).trim();
                    }

                    if (!ImageStorage.isValidImageId(user.getUserId(), publicId)) {
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thông tin ảnh không hợp lệ");
                        return;
                    }

                    if (!ImageStorage.checkIsOwnImage(user.getUserId(), publicId)) {
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Bạn không phải là chủ của bức ảnh");
                        return;
                    }

                    if (!ImageStorage.existsInTmp(publicId)) {
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ảnh không tồn tại hoặc đã hết hạn, vui lòng tải lại");
                        return;
                    }
                }
            }
            if (publicId.isEmpty()) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Chưa chọn ảnh nào");
                return;
            }

            List<String> publicIds = new ArrayList<String>();
            publicIds.add(publicId);
            if (!ImageStorage.moveToFinal(ImageType.VOUCHER_TYPE ,publicIds)) {
                _Logger.error("move images failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            TVoucher voucher = new TVoucher();
            voucher.setCreatedAt(System.currentTimeMillis());
            voucher.setTitle(title);
            voucher.setCode(code);
            voucher.setBody(bodyFromParam);
            voucher.setDiscountPercent(discountPercent);
            voucher.setMaxDiscount(maxDiscount);
            voucher.setPublicId(publicId);
            voucher.setStartDate(startDate);
            voucher.setEndDate(endDate);

            TVoucherResult result = null;
            boolean saved = false;
            try {
                result = ClientHolder.get().createVoucher(voucher);
                saved = !Err.isFail(result.getError());
            } finally {
                if (!saved) {
                    ImageStorage.moveBackToTmp(ImageType.VOUCHER_TYPE ,publicIds);
                }
            }
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
            if(result.getValue() != null) result.getValue().setImageUrl(ImageStorage.buildUrl(ImageType.VOUCHER_TYPE, publicId));
            ok(resp, result.getValue());
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
            if (!hasRole(req, resp, Role.VOUCHER_WRITE)) {
                return;
            }
            TUser user = getUserFromRequest(req);
            Integer voucherId = parseIdFromRequest(req);
            if (voucherId == null || voucherId <= 0) {
                _Logger.info("Bad request voucherId " + voucherId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            Map<String, Object> body = jsonBody(req);
            String title = param(req, body, "title");
            String code = param(req, body, "code");
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

            String publicId = "";
            Object rawImages = body == null ? null : body.get("images");
            if (rawImages != null) {
                if (!(rawImages instanceof List) || ((List<?>) rawImages).size() > 1) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Voucher chỉ được phép có duy nhất 1 ảnh");
                    return;
                }
                for (Object o : (List<?>) rawImages) {
                    if (o instanceof Map && ((Map<?, ?>) o).get("publicId") != null) {
                        publicId = String.valueOf(((Map<?, ?>) o).get("publicId")).trim();
                    }
                }
            }
            if (publicId.isEmpty()) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Chưa chọn ảnh nào");
                return;
            }

            TVoucherResult voucherResult = ClientHolder.get().getVoucherById(voucherId);
            if (Err.isFail(voucherResult.getError())) {
                if (Err.isNetworkError(voucherResult.getError())) {
                    _Logger.error("Network error, get voucher");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(voucherResult.getError())) {
                    _Logger.info("Notfound, get voucher");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy voucher");
                    return;
                }
                _Logger.error("get voucher failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            String oldPublicId = null;
            if (voucherResult.getValue() != null) {
                oldPublicId = voucherResult.getValue().getPublicId();
            }

            boolean imageChanged = !publicId.equals(oldPublicId);
            List<String> newIds = new ArrayList<String>();
            if (imageChanged) {
                if (!ImageStorage.isValidImageId(user.getUserId(), publicId)) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thông tin ảnh không hợp lệ");
                    return;
                }
                if (!ImageStorage.checkIsOwnImage(user.getUserId(), publicId)) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Bạn không phải là chủ của bức ảnh");
                    return;
                }
                if (!ImageStorage.existsInTmp(publicId)) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ảnh không tồn tại hoặc đã hết hạn, vui lòng tải lại");
                    return;
                }
                newIds.add(publicId);
                if (!ImageStorage.moveToFinal(ImageType.VOUCHER_TYPE, newIds)) {
                    _Logger.error("move images failed");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                    return;
                }
            }

            TVoucher voucher = new TVoucher();
            voucher.setVoucherId(voucherId);
            voucher.setTitle(title);
            voucher.setCode(code);
            voucher.setBody(bodyFromParam);
            voucher.setDiscountPercent(discountPercent);
            voucher.setMaxDiscount(maxDiscount);
            voucher.setPublicId(publicId);
            voucher.setStartDate(startDate);
            voucher.setEndDate(endDate);

            TVoucherResult result = null;
            boolean saved = false;
            try {
                result = ClientHolder.get().updateVoucher(voucher);
                saved = !Err.isFail(result.getError());
            } finally {
                if (!saved && imageChanged) {
                    ImageStorage.moveBackToTmp(ImageType.VOUCHER_TYPE, newIds);
                }
            }
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

            if (imageChanged && oldPublicId != null && !oldPublicId.isEmpty()) {
                try {
                    ImageStorage.deleteFromFinal(ImageType.VOUCHER_TYPE, oldPublicId);
                } catch (Exception e) {
                    _Logger.error("delete old voucher image failed: " + oldPublicId, e);
                }
            }
            if(result.getValue() != null) voucher.setImageUrl(ImageStorage.buildUrl(ImageType.VOUCHER_TYPE, publicId));
            ok(resp, voucher);
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
            if (!hasRole(req, resp, Role.VOUCHER_WRITE)) {
                return;
            }
            Integer voucherId = parseIdFromRequest(req);
            if (voucherId == null || voucherId <= 0) {
                _Logger.info("Bad request voucherId " + voucherId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }

            TVoucherResult getResult = ClientHolder.get().getVoucherById(voucherId);
            if (Err.isFail(getResult.getError())) {
                if (Err.isNetworkError(getResult.getError())) {
                    _Logger.error("Network error, get voucher before delete");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(getResult.getError())) {
                    _Logger.info("Notfound, get voucher before delete");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy ưu đãi để xóa");
                    return;
                }
                _Logger.error("Get voucher before delete failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            String publicId = null;
            if (getResult.getValue() != null) {
                publicId = getResult.getValue().getPublicId();
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

            if (publicId != null && !publicId.isEmpty()) {
                try {
                    ImageStorage.deleteFromFinal(ImageType.VOUCHER_TYPE, publicId);
                } catch (Exception e) {
                    _Logger.error("delete voucher image file failed: " + publicId, e);
                }
            }
            ok(resp, "Xóa dữ liệu thành công");
        } catch (Exception e) {
            _Logger.error("Delete voucher failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}
