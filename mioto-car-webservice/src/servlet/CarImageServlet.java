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
import thrift.TCarImage;
import thrift.TCarImageResult;
import thrift.TListCarImageResult;
import thrift.TUser;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class CarImageServlet extends AuthServlet {

    // POST body: {"carId": 7, "images": [{"imageUrl": "...", "publicId": "..."}]}
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            TUser user = getUserFromRequest(req);
            Map<String, Object> body = jsonBody(req);
            _Logger.info("DEBUG body = " + body + ", carId = " + param(req, body, "carId"));           
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

            Object rawImages = body == null ? null : body.get("images");
            if (rawImages == null || !(rawImages instanceof List) || ((List<?>) rawImages).isEmpty() || ((List<?>) rawImages).size() > 5) {
                _Logger.info("Bad request images");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mỗi lần thêm từ 1 đến 5 ảnh");
                return;
            }
            List<TCarImage> images = new ArrayList<TCarImage>();
            for (Object o : (List<?>) rawImages) {
                if (!(o instanceof Map)) {
                    _Logger.info("Bad request image item");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thông tin ảnh không hợp lệ");
                    return;
                }
                Map<?, ?> m = (Map<?, ?>) o;
                String imageUrl = m.get("imageUrl") == null ? "" : String.valueOf(m.get("imageUrl")).trim();
                String publicId = m.get("publicId") == null ? "" : String.valueOf(m.get("publicId")).trim();
                if (imageUrl.isEmpty() || publicId.isEmpty() || imageUrl.length() > 255 || publicId.length() > 100) {
                    _Logger.info("Bad request image item");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thông tin ảnh không hợp lệ");
                    return;
                }
                TCarImage image = new TCarImage();
                image.setCarId(carId);               // model lấy carId từ phần tử đầu
                image.setImageUrl(imageUrl);
                image.setPublicId(publicId);
                image.setCreatedAt(System.currentTimeMillis());
                images.add(image);
            }

            TListCarImageResult result = ClientHolder.get().createCarImages(images, (int) user.getUserId());
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, create car images");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (result.getError() == Err.FORBIDDEN) {
                    _Logger.info("Forbidden, create car images, carId=" + carId);
                    fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN, "Bạn không phải chủ của xe này");
                    return;
                }
                _Logger.error("create car images failed");
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
            _Logger.error("create car images failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    // DELETE body: {"carId": 7, "imageIds": [1, 2, 3]}
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

            Object rawIds = body == null ? null : body.get("imageIds");
            if (rawIds == null || !(rawIds instanceof List) || ((List<?>) rawIds).isEmpty() || ((List<?>) rawIds).size() > 100) {
                _Logger.info("Bad request imageIds");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách ảnh cần xóa không hợp lệ");
                return;
            }
            List<Long> imageIds = new ArrayList<Long>();
            for (Object o : (List<?>) rawIds) {
                if (!(o instanceof Number) || ((Number) o).longValue() <= 0) {
                    _Logger.info("Bad request imageId item");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id ảnh không hợp lệ");
                    return;
                }
                imageIds.add(((Number) o).longValue());
            }

            TCarImageResult result = ClientHolder.get().deleteCarImages(carId, imageIds, (int) user.getUserId());
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, delete car images");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (result.getError() == Err.FORBIDDEN) {
                    _Logger.info("Forbidden, delete car images, carId=" + carId);
                    fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN, "Bạn không phải chủ của xe này");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, delete car images");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy ảnh để xóa");
                    return;
                }
                _Logger.error("delete car images failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, "Xóa dữ liệu thành công");
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("delete car images failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}    