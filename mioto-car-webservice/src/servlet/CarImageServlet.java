/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import imageType.ImageType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import thrift.TCarImage;
import thrift.TCarImagePublicIdResult;
import thrift.TCarImageResult;
import thrift.TListCarImageResult;
import thrift.TUser;
import util.ClientHolder;
import util.ImageStorage;

/**
 *
 * @author tuanlee
 */
public class CarImageServlet extends AuthWithoutGetServlet {

    // POST body: {"carId": 7, "images": [{"publicId": "..."}]}
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

            long count = ClientHolder.get().countImageOfCar(carId);
            if(Err.isFail(count))
            {
                if(Err.isNetworkError(count))
                {
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            
            // images: [{"publicId": "..."}, ...]
            List<TCarImage> images = new ArrayList<TCarImage>();
            List<String> imageIds = new ArrayList<String>();
            Object rawImages = body == null ? null : body.get("images");

            if (rawImages != null) {
                // phải là danh sách, tối đa 5 ảnh
                if (!(rawImages instanceof List)) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách ảnh không hợp lệ");
                    return;
                }
                int newCount = ((List<?>) rawImages).size();
                if (count + newCount > ImageStorage.MAX_IMAGES) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST,
                            "Xe đã có " + count + " ảnh, chỉ được tối đa " + ImageStorage.MAX_IMAGES
                            + " ảnh. Hãy xóa bớt ảnh cũ trước");
                    return;
                }
                Set<String> seen = new HashSet<String>();
                for (Object o : (List<?>) rawImages) {
                    // lấy publicId từ từng phần tử
                    String publicId = "";
                    if (o instanceof Map && ((Map<?, ?>) o).get("publicId") != null) {
                        publicId = String.valueOf(((Map<?, ?>) o).get("publicId")).trim();
                    }

                    // đúng định dạng 
                    if (!ImageStorage.isValidImageId(user.getUserId(), publicId)) {
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thông tin ảnh không hợp lệ");
                        return;
                    }
                    
                    //đúng chủ ảnh
                    if(!ImageStorage.checkIsOwnImage(user.getUserId(), publicId))
                    {
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Bạn không phải là chủ của bức ảnh");
                        return;
                    }
                    // file có trong tmp không?
                    if (!ImageStorage.existsInTmp(publicId)) {
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ảnh không tồn tại hoặc đã hết hạn, vui lòng tải lại");
                        return;
                    }
                    // trùng nhau không?
                    if (!seen.add(publicId)) {
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách ảnh bị trùng");
                        return;
                    }

                    TCarImage image = new TCarImage();
                    image.setCarId(carId);
                    image.setPublicId(publicId);
                    image.setCreatedAt(System.currentTimeMillis());
                    images.add(image);
                    imageIds.add(publicId);
                }
            }
            if (imageIds.isEmpty()) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Chưa chọn ảnh nào");
                return;
            }
            if (!ImageStorage.moveToFinal(ImageType.CAR_TYPE ,imageIds)) {
                _Logger.error("move images failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }

            TListCarImageResult result = null;
            boolean saved = false;
            try {
                result = ClientHolder.get().createCarImages(images, (int) user.getUserId());
                saved = !Err.isFail(result.getError());
            } finally {
                if (!saved) {
                    ImageStorage.moveBackToTmp(ImageType.CAR_TYPE ,imageIds);
                }
            }

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

            // ---------- carId ----------
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
            if (!(rawIds instanceof List) || ((List<?>) rawIds).isEmpty()
                    || ((List<?>) rawIds).size() > ImageStorage.MAX_IMAGES) {
                _Logger.info("Bad request imageIds");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách ảnh cần xóa không hợp lệ");
                return;
            }
            Set<Long> idSet = new LinkedHashSet<Long>();
            for (Object o : (List<?>) rawIds) {
                if (!(o instanceof Number) || ((Number) o).longValue() <= 0) {
                    _Logger.info("Bad request imageId item");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id ảnh không hợp lệ");
                    return;
                }
                idSet.add(((Number) o).longValue());
            }
            List<Long> imageIds = new ArrayList<Long>(idSet);

            TCarImagePublicIdResult pubResult = ClientHolder.get().multigetPublicId(carId, imageIds);
            if (Err.isFail(pubResult.getError())) {
                if (Err.isNetworkError(pubResult.getError())) {
                    _Logger.error("Network error, get publicIds");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.BAD_REQUEST == pubResult.getError()) {
                    _Logger.info("Bad request, get publicIds, carId=" + carId);
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thông tin ảnh không hợp lệ");
                    return;
                }
                _Logger.error("get publicIds failed, carId=" + carId);
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }

            List<String> publicIds = pubResult.getPublicIds();
            if (publicIds == null || publicIds.isEmpty()) {
                _Logger.info("Notfound, get publicIds, carId=" + carId);
                fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy thông tin ảnh để xóa");
                return;
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

            for (String publicId : publicIds) {
                try {
                    ImageStorage.deleteFromFinal(ImageType.CAR_TYPE ,publicId);
                } catch (Exception e) {
                    _Logger.error("delete image file failed: " + publicId, e);
                }
            }
            ok(resp, "Xóa dữ liệu thành công");
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } catch (Exception e) {
            _Logger.error("delete car images failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }    
}
