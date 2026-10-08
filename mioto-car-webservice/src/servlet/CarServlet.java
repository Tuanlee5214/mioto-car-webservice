/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import role.Role;
import thrift.TCar;
import thrift.TCarDetailResult;
import thrift.TCarFeature;
import thrift.TCarFilterRequest;
import thrift.TCarImage;
import thrift.TCarResult;
import thrift.TCarStatus;
import thrift.TCarView;
import thrift.TListCarViewResult;
import thrift.TTransmission;
import thrift.TTypeFuel;
import thrift.TUser;
import util.ClientHolder;

/**
 *
 * @author tuanlee
 */
public class CarServlet extends AuthServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String path = req.getPathInfo();   // null, "/mine", "/5"
            int count = 20;
            int offset = 0;

            // ---------- GET /api/cars/{id} ----------
            if (path != null && !path.equals("/") && !path.equals("/mine") && !path.equals("/admin")) {
                int carId = Integer.parseInt(path.substring(1));
                if (carId <= 0) {
                    _Logger.info("Bad request carId " + carId);
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                    return;
                }
                TCarDetailResult result = ClientHolder.get().getCarById(carId);
                if (Err.isFail(result.getError())) {
                    if (Err.isNetworkError(result.getError())) {
                        _Logger.error("Network error, get car by id");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    }
                    if (Err.isNotFound(result.getError())) {
                        _Logger.info("Notfound, get car by id");
                        fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy xe");
                        return;
                    }
                    _Logger.error("get car by id failed");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                    return;
                }
                TCarView view = result.getValue().getCarView();
                TCar car = view.getCar();
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("carId", car.getCarId());
                data.put("carName", car.getCarName());
                data.put("productionYear", car.getProductionYear());
                data.put("carBrandId", car.getCarBrandId());
                data.put("brandName", view.getBrandName());
                data.put("numSeats", car.getNumSeats());
                data.put("transmission", car.getTransmission());
                data.put("typeFuel", car.getTypeFuel());
                data.put("fuelConsumption", car.getFuelConsumption());
                data.put("description", car.getDescription());
                data.put("pricePerDay", car.getPricePerDay());
                data.put("policy", car.getPolicy());
                data.put("districtId", car.getDistrictId());
                data.put("districtName", view.getDistrictName());
                data.put("provinceName", view.getProvinceName());
                data.put("userId", car.getUserId());
                data.put("ownerName", view.getOwnerName());
                data.put("status", car.getStatus());
                data.put("createdAt", car.getCreatedAt());
                data.put("updatedAt", car.getUpdatedAt());
                data.put("images", result.getValue().getImages() != null ? result.getValue().getImages() : new ArrayList<TCarImage>());
                data.put("features", result.getValue().getFeatures() != null ? result.getValue().getFeatures() : new ArrayList<TCarImage>());
                data.put("unavails", result.getValue().getUnavails() != null ? result.getValue().getUnavails() : new ArrayList<TCarImage>());
                ok(resp, data);
                return;
            }

            // ---------- đọc count/offset ----------
            String countFromParam = param(req, null, "count");
            String offsetFromParam = param(req, null, "offset");
            if (countFromParam != null) {
                count = Integer.parseInt(countFromParam);
            }
            if (offsetFromParam != null) {
                offset = Integer.parseInt(offsetFromParam);
            }
            count = (count <= 0 || count > 100) ? 20 : count;
            offset = Math.max(0, offset);

            TListCarViewResult result;

            // ---------- GET /api/cars/mine ----------
            if ("/mine".equals(path)) {
                TUser user = getUserFromRequest(req);
                result = ClientHolder.get().getCarViewByUserId((int) user.getUserId(), count, offset);
            } // ---------- GET /api/cars  và  GET /api/cars/admin ----------
            else {  
                if ("/admin".equals(path)) {
                    return;
                }
                TCarFilterRequest filter = new TCarFilterRequest();
                String carName = param(req, null, "carName");
                String productionYear = param(req, null, "productionYear");
                String carBrandId = param(req, null, "carBrandId");
                String numSeats = param(req, null, "numSeats");
                String transmission = param(req, null, "transmission");
                String typeFuel = param(req, null, "typeFuel");
                String fuelConsumption = param(req, null, "fuelConsumption");
                String pricePerDay = param(req, null, "pricePerDay");
                String districtId = param(req, null, "districtId");
                String provinceId = param(req, null, "provinceId");
                String startTimeFromParam = param(req, null, "startTime");
                String endTimeFromParam = param(req, null, "endTime");
                String statusFromParam = param(req, null, "status");

                if (carName != null) {
                    filter.setCarName(carName);
                }
                if (productionYear != null) {
                    filter.setProductionYear(Integer.parseInt(productionYear));
                }
                if (carBrandId != null) {
                    filter.setCarBrandId(Integer.parseInt(carBrandId));
                }
                if (numSeats != null) {
                    filter.setNumSeats((byte) Integer.parseInt(numSeats));
                }
                if (transmission != null) {
                    TTransmission t = TTransmission.findByValue(Integer.parseInt(transmission));
                    if (t == null) {
                        _Logger.info("Bad request transmission " + transmission);
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Hộp số không hợp lệ");
                        return;
                    }
                    filter.setTransmission(t);
                }
                if (typeFuel != null) {
                    TTypeFuel t = TTypeFuel.findByValue(Integer.parseInt(typeFuel));
                    if (t == null) {
                        _Logger.info("Bad request typeFuel " + typeFuel);
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Loại nhiên liệu không hợp lệ");
                        return;
                    }
                    filter.setTypeFuel(t);
                }
                if (fuelConsumption != null) {
                    filter.setFuelConsumption(new BigDecimal(fuelConsumption).toPlainString());
                }
                if (pricePerDay != null) {
                    filter.setPricePerDay(new BigDecimal(pricePerDay).toPlainString());
                }
                if (districtId != null) {
                    filter.setDistrictId(Integer.parseInt(districtId));
                }
                if (provinceId != null) {
                    filter.setProvinceId(Integer.parseInt(provinceId));
                }

                if ("/admin".equals(path)) {
                    if(!hasRole(req, resp, Role.CAR_READ)) return;
                    int status = statusFromParam == null ? 0 : Integer.parseInt(statusFromParam);   // 0 = tất cả
                    if (status != 0 && TCarStatus.findByValue(status) == null) {
                        _Logger.info("Bad request status " + status);
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Trạng thái không hợp lệ");
                        return;
                    }
                    result = ClientHolder.get().searchCarByAdmin(filter, status, count, offset);
                } else {
                    long startTime = startTimeFromParam == null ? 0 : Long.parseLong(startTimeFromParam);
                    long endTime = endTimeFromParam == null ? 0 : Long.parseLong(endTimeFromParam);
                    if ((startTime > 0 || endTime > 0) && (startTime <= 0 || endTime <= startTime)) {
                        _Logger.info("Bad request time " + startTime + " - " + endTime);
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Khoảng thời gian thuê không hợp lệ");
                        return;
                    }
                    result = ClientHolder.get().searchCar(filter, startTime, endTime, count, offset);
                }
            }

            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, get list car");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                _Logger.error("get list car failed");
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
        } catch (Exception e) {
            _Logger.error("get car failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            TUser user = getUserFromRequest(req);
            Map<String, Object> body = jsonBody(req);
            String carName = param(req, body, "carName");
            String productionYearFromParam = param(req, body, "productionYear");
            String carBrandIdFromParam = param(req, body, "carBrandId");
            String numSeatsFromParam = param(req, body, "numSeats");
            String transmissionFromParam = param(req, body, "transmission");
            String typeFuelFromParam = param(req, body, "typeFuel");
            String fuelConsumptionFromParam = param(req, body, "fuelConsumption");
            String description = param(req, body, "description");
            String pricePerDayFromParam = param(req, body, "pricePerDay");
            String policy = param(req, body, "policy");
            String districtIdFromParam = param(req, body, "districtId");

            if (carName == null || carName.trim().isEmpty()) {
                _Logger.info("Bad request carName");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên xe không được để trống");
                return;
            }
            if (productionYearFromParam == null || productionYearFromParam.trim().isEmpty()) {
                _Logger.info("Bad request productionYear");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Năm sản xuất không được để trống");
                return;
            }
            if (carBrandIdFromParam == null || carBrandIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request carBrandId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Hãng xe không được để trống");
                return;
            }
            if (numSeatsFromParam == null || numSeatsFromParam.trim().isEmpty()) {
                _Logger.info("Bad request numSeats");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Số ghế không được để trống");
                return;
            }
            if(fuelConsumptionFromParam == null || fuelConsumptionFromParam.trim().isEmpty())
            {
                _Logger.info("Bad request fuel consumption");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mức tiêu thụ nhiên liệu không được để trống");
                return;
            }
            if (transmissionFromParam == null || transmissionFromParam.trim().isEmpty()) {
                _Logger.info("Bad request transmission");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Hộp số không được để trống");
                return;
            }
            if (typeFuelFromParam == null || typeFuelFromParam.trim().isEmpty()) {
                _Logger.info("Bad request typeFuel");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Loại nhiên liệu không được để trống");
                return;
            }
            if (pricePerDayFromParam == null || pricePerDayFromParam.trim().isEmpty()) {
                _Logger.info("Bad request pricePerDay");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Giá thuê không được để trống");
                return;
            }
            if (districtIdFromParam == null || districtIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request districtId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Quận/huyện không được để trống");
                return;
            }
            if(description == null || description.trim().isEmpty()) {
                _Logger.info("Bad request description");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mô tả không được để trống");
                return;
            }
            if(policy == null || policy.trim().isEmpty())
            {
                _Logger.info("Bad request policy");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Chính sách không được để trống");
                return;
            }

            int productionYear = Integer.parseInt(productionYearFromParam);
            int carBrandId = Integer.parseInt(carBrandIdFromParam);
            int numSeats = Integer.parseInt(numSeatsFromParam);
            int transmission = Integer.parseInt(transmissionFromParam);
            int typeFuel = Integer.parseInt(typeFuelFromParam);
            int districtId = Integer.parseInt(districtIdFromParam);
            BigDecimal fuelConsumption = new BigDecimal(fuelConsumptionFromParam == null ? "0" : fuelConsumptionFromParam);
            BigDecimal pricePerDay = new BigDecimal(pricePerDayFromParam);

            if (carName.length() > 150) {
                _Logger.info("Bad request carName length " + carName.length());
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên xe tối đa 150 ký tự");
                return;
            }
            if (productionYear < 1900 || productionYear > Calendar.getInstance().get(Calendar.YEAR) + 1) {
                _Logger.info("Bad request productionYear : " + productionYear);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Năm sản xuất không hợp lệ");
                return;
            }
            if (carBrandId <= 0) {
                _Logger.info("Bad request carBrandId : " + carBrandId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Hãng xe không hợp lệ");
                return;
            }
            if (numSeats <= 0 || numSeats > 127) {
                _Logger.info("Bad request numSeats : " + numSeats);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Số ghế không hợp lệ");
                return;
            }
            if (TTransmission.findByValue(transmission) == null) {
                _Logger.info("Bad request transmission : " + transmission);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Hộp số không hợp lệ");
                return;
            }
            if (TTypeFuel.findByValue(typeFuel) == null) {
                _Logger.info("Bad request typeFuel : " + typeFuel);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Loại nhiên liệu không hợp lệ");
                return;
            }
            if (fuelConsumption.signum() < 0 || fuelConsumption.compareTo(new BigDecimal("999.9")) > 0) {
                _Logger.info("Bad request fuelConsumption : " + fuelConsumption);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mức tiêu thụ nhiên liệu không hợp lệ");
                return;
            }
            if (pricePerDay.signum() <= 0) {
                _Logger.info("Bad request pricePerDay : " + pricePerDay);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Giá thuê không hợp lệ");
                return;
            }
            if (districtId <= 0) {
                _Logger.info("Bad request districtId : " + districtId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Quận/huyện không hợp lệ");
                return;
            }

            // images: [{"imageUrl": "...", "publicId": "..."}, ...]
            List<TCarImage> images = new ArrayList<TCarImage>();
            Object rawImages = body == null ? null : body.get("images");
            if (rawImages != null) {
                if (!(rawImages instanceof List) || ((List<?>) rawImages).size() > 5) {
                    _Logger.info("Bad request images");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách ảnh không hợp lệ (tối đa 5 ảnh)");
                    return;
                }
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
                    image.setImageUrl(imageUrl);
                    image.setPublicId(publicId);
                    image.setCreatedAt(System.currentTimeMillis());
                    images.add(image);
                }
            }
            // features: [1, 2, 3]
            List<TCarFeature> features = new ArrayList<TCarFeature>();
            Object rawFeatures = body == null ? null : body.get("features");
            if (rawFeatures != null) {
                if (!(rawFeatures instanceof List)) {
                    _Logger.info("Bad request features");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Danh sách tính năng không hợp lệ");
                    return;
                }
                Set<Integer> featureIds = new LinkedHashSet<Integer>();   // loại trùng vì bảng có UNIQUE(carId, featureId)
                for (Object o : (List<?>) rawFeatures) {
                    int featureId = (int) Double.parseDouble(String.valueOf(o).trim());
                    if (featureId <= 0) {
                        _Logger.info("Bad request featureId : " + featureId);
                        fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tính năng không hợp lệ");
                        return;
                    }
                    featureIds.add(featureId);
                }
                for (Integer featureId : featureIds) {
                    TCarFeature feature = new TCarFeature();
                    feature.setFeatureId(featureId);
                    features.add(feature);
                }
            }

            TCar car = new TCar();
            car.setCarName(carName);
            car.setProductionYear(productionYear);
            car.setCarBrandId(carBrandId);
            car.setNumSeats((byte) numSeats);
            car.setTransmission(transmission);
            car.setTypeFuel(typeFuel);
            car.setFuelConsumption(fuelConsumption.toPlainString());
            car.setPricePerDay(pricePerDay.toPlainString());
            car.setDistrictId(districtId);
            car.setDescription(description);
            car.setPolicy(policy);
            car.setStatus(TCarStatus.TC_PENDING.getValue());
            car.setUserId(user.getUserId());               // lấy từ session
            car.setCreatedAt(System.currentTimeMillis());
            car.setUpdatedAt(System.currentTimeMillis());

            TCarResult result = ClientHolder.get().createCar(car, images, features);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, create car");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                _Logger.error("create car failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("create car failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String path = req.getPathInfo();   // "/5" hoặc "/5/status"
            if (path == null || path.equals("/")) {
                _Logger.info("Bad request path " + path);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            String[] seg = path.substring(1).split("/");
            int carId = Integer.parseInt(seg[0]);
            if (carId <= 0) {
                _Logger.info("Bad request carId " + carId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Id không hợp lệ");
                return;
            }
            Map<String, Object> body = jsonBody(req);

            // ---------- PUT /api/cars/{id}/status (admin) ----------
            if (seg.length == 2 && "status".equals(seg[1])) {
                if(!hasRole(req, resp, Role.CAR_WRITE)) return;
                String statusFromParam = param(req, body, "status");
                String userIdFromParam = param(req, body, "userId");
                if (statusFromParam == null || statusFromParam.trim().isEmpty()) {
                    _Logger.info("Bad request status");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Trạng thái không được để trống");
                    return;
                }
                if (userIdFromParam == null || statusFromParam.trim().isEmpty()) {
                    _Logger.info("Bad request userId");
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Chủ xe không được để trống");
                    return;
                }
                int status = Integer.parseInt(statusFromParam);
                int userId = Integer.parseInt(userIdFromParam);
                if (TCarStatus.findByValue(status) == null) {
                    _Logger.info("Bad request status : " + status);
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Trạng thái không hợp lệ");
                    return;
                }
                if (userId <= 0) {
                    _Logger.info("Bad request userId : " + userId);
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Chủ xe không hợp lệ");
                    return;
                }

                TCar car = new TCar();
                car.setCarId(carId);
                car.setUserId(userId);
                car.setStatus(status);
                TCarResult result = ClientHolder.get().updateStatusCar(car);
                if (Err.isFail(result.getError())) {
                    if (Err.isNetworkError(result.getError())) {
                        _Logger.error("Network error, update status car");
                        fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                        return;
                    }
                    if (Err.isNotFound(result.getError())) {
                        _Logger.info("Notfound, update status car");
                        fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy xe hoặc chủ xe");
                        return;
                    }
                    _Logger.error("update status car failed");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                    return;
                }
                ok(resp, result.getValue() != null ? result.getValue() : result.getMessage());
                return;
            }

            // ---------- PUT /api/cars/{id} (chủ xe) ----------
            if (seg.length != 1) {
                _Logger.info("Bad request path " + path);
                fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy đường dẫn");
                return;
            }
            TUser user = getUserFromRequest(req);
            String carName = param(req, body, "carName");
            String productionYearFromParam = param(req, body, "productionYear");
            String carBrandIdFromParam = param(req, body, "carBrandId");
            String numSeatsFromParam = param(req, body, "numSeats");
            String transmissionFromParam = param(req, body, "transmission");
            String typeFuelFromParam = param(req, body, "typeFuel");
            String fuelConsumptionFromParam = param(req, body, "fuelConsumption");
            String description = param(req, body, "description");
            String pricePerDayFromParam = param(req, body, "pricePerDay");
            String policy = param(req, body, "policy");
            String districtIdFromParam = param(req, body, "districtId");

            if (carName == null || carName.trim().isEmpty()) {
                _Logger.info("Bad request carName");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên xe không được để trống");
                return;
            }
            if (productionYearFromParam == null || productionYearFromParam.trim().isEmpty()) {
                _Logger.info("Bad request productionYear");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Năm sản xuất không được để trống");
                return;
            }
            if (carBrandIdFromParam == null || carBrandIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request carBrandId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Hãng xe không được để trống");
                return;
            }
            if(fuelConsumptionFromParam == null || fuelConsumptionFromParam.trim().isEmpty())
            {
                _Logger.info("Bad request fuel consumption");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mức tiêu thụ nhiên liệu không được để trống");
                return;
            }
            if (numSeatsFromParam == null || numSeatsFromParam.trim().isEmpty()) {
                _Logger.info("Bad request numSeats");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Số ghế không được để trống");
                return;
            }
            if (transmissionFromParam == null || transmissionFromParam.trim().isEmpty()) {
                _Logger.info("Bad request transmission");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Hộp số không được để trống");
                return;
            }
            if (typeFuelFromParam == null || typeFuelFromParam.trim().isEmpty()) {
                _Logger.info("Bad request typeFuel");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Loại nhiên liệu không được để trống");
                return;
            }
            if (pricePerDayFromParam == null || pricePerDayFromParam.trim().isEmpty()) {
                _Logger.info("Bad request pricePerDay");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Giá thuê không được để trống");
                return;
            }
            if (districtIdFromParam == null || districtIdFromParam.trim().isEmpty()) {
                _Logger.info("Bad request districtId");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Quận/huyện không được để trống");
                return;
            }
            if(description == null || description.trim().isEmpty()) {
                _Logger.info("Bad request description");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mô tả không được để trống");
                return;
            }
            if(policy == null || policy.trim().isEmpty())
            {
                _Logger.info("Bad request policy");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Chính sách không được để trống");
                return;
            }


            int productionYear = Integer.parseInt(productionYearFromParam);
            int carBrandId = Integer.parseInt(carBrandIdFromParam);
            int numSeats = Integer.parseInt(numSeatsFromParam);
            int transmission = Integer.parseInt(transmissionFromParam);
            int typeFuel = Integer.parseInt(typeFuelFromParam);
            int districtId = Integer.parseInt(districtIdFromParam);
            BigDecimal fuelConsumption = new BigDecimal(fuelConsumptionFromParam == null ? "0" : fuelConsumptionFromParam);
            BigDecimal pricePerDay = new BigDecimal(pricePerDayFromParam);

            if (carName.length() > 150) {
                _Logger.info("Bad request carName length " + carName.length());
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên xe tối đa 150 ký tự");
                return;
            }
            if (productionYear < 1900 || productionYear > Calendar.getInstance().get(Calendar.YEAR) + 1) {
                _Logger.info("Bad request productionYear : " + productionYear);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Năm sản xuất không hợp lệ");
                return;
            }
            if (carBrandId <= 0) {
                _Logger.info("Bad request carBrandId : " + carBrandId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Hãng xe không hợp lệ");
                return;
            }
            if (numSeats <= 0 || numSeats > 127) {
                _Logger.info("Bad request numSeats : " + numSeats);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Số ghế không hợp lệ");
                return;
            }
            if (TTransmission.findByValue(transmission) == null) {
                _Logger.info("Bad request transmission : " + transmission);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Hộp số không hợp lệ");
                return;
            }
            if (TTypeFuel.findByValue(typeFuel) == null) {
                _Logger.info("Bad request typeFuel : " + typeFuel);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Loại nhiên liệu không hợp lệ");
                return;
            }
            if (fuelConsumption.signum() < 0 || fuelConsumption.compareTo(new BigDecimal("999.9")) > 0) {
                _Logger.info("Bad request fuelConsumption : " + fuelConsumption);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mức tiêu thụ nhiên liệu không hợp lệ");
                return;
            }
            if (pricePerDay.signum() <= 0) {
                _Logger.info("Bad request pricePerDay : " + pricePerDay);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Giá thuê không hợp lệ");
                return;
            }
            if (districtId <= 0) {
                _Logger.info("Bad request districtId : " + districtId);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Quận/huyện không hợp lệ");
                return;
            }
            TCar car = new TCar();
            car.setCarId(carId);
            car.setCarName(carName);
            car.setProductionYear(productionYear);
            car.setCarBrandId(carBrandId);
            car.setNumSeats((byte) numSeats);
            car.setTransmission(transmission);
            car.setTypeFuel(typeFuel);
            car.setFuelConsumption(fuelConsumption.toPlainString());
            car.setPricePerDay(pricePerDay.toPlainString());
            car.setDistrictId(districtId);
            car.setDescription(description);
            car.setPolicy(policy);
            car.setStatus(TCarStatus.TC_PENDING.getValue());
            car.setUserId(user.getUserId());               // lấy từ session
            car.setUpdatedAt(System.currentTimeMillis());

            TCarResult result = ClientHolder.get().updateCar(car);
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, update car");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (result.getError() == Err.FORBIDDEN) {
                    _Logger.info("Forbidden, update car, carId=" + carId);
                    fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN, "Bạn không phải chủ của xe này");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, update car");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy xe để cập nhật");
                    return;
                }
                _Logger.error("update car failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            ok(resp, result.getValue());
        } catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
        } catch (Exception e) {
            _Logger.error("update car failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}
