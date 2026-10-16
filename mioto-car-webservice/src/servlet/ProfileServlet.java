/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import imageType.ImageType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import thrift.TUpdateUserResult;
import thrift.TUser;
import thrift.TUserResult;
import util.ClientHolder;
import util.ImageStorage;

/**
 *
 * @author tuanlee
 */
public class ProfileServlet extends AuthServlet {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

        @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    {
        try
        {
            Map<String, Object> body = jsonBody(req);
            String email = param(req, body, "email");
            String name = param(req, body, "displayName");            
            if(name == null || name.isEmpty())
            {
                _Logger.info("Name is blank, name = " + name);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tên không được để trống");
                return;
            }
            Matcher macher = EMAIL_PATTERN.matcher(email);
            if(!macher.matches()){
                _Logger.info("Email is incorrect format, email = " + email);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Email không đúng định dạng");
                return;
            }
            
            String publicId = "";
            Object rawImages = body == null ? null : body.get("images");
            if (rawImages != null) {
                if (!(rawImages instanceof List) || ((List<?>) rawImages).size() > 1) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ảnh đại diện chỉ được phép có duy nhất 1 ảnh");
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

            TUserResult userResult = ClientHolder.get().getUser(getUserFromRequest(req).getUserId());
            if (Err.isFail(userResult.getError())) {
                if (Err.isNetworkError(userResult.getError())) {
                    _Logger.error("Network error, get user");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(userResult.getError())) {
                    _Logger.info("Notfound, get user");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy user");
                    return;
                }
                _Logger.error("get user failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            String oldPublicId = null;
            if (userResult.getValue() != null) {
                oldPublicId = userResult.getValue().getPublicId();
            }

            boolean imageChanged = !publicId.equals(oldPublicId);
            List<String> newIds = new ArrayList<String>();
            if (imageChanged) {
                if (!ImageStorage.isValidImageId(userResult.getValue().getUserId(), publicId)) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Thông tin ảnh không hợp lệ");
                    return;
                }
                if (!ImageStorage.checkIsOwnImage(userResult.getValue().getUserId(), publicId)) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Bạn không phải là chủ của bức ảnh");
                    return;
                }
                if (!ImageStorage.existsInTmp(publicId)) {
                    fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Ảnh không tồn tại hoặc đã hết hạn, vui lòng tải lại");
                    return;
                }
                newIds.add(publicId);
                if (!ImageStorage.moveToFinal(ImageType.PROFILE_TYPE, newIds)) {
                    _Logger.error("move images failed");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                    return;
                }
            }

            
            TUser user = new TUser();
            user.setUserId(getUserFromRequest(req).getUserId());
            user.setDisplayName(name);
            user.setPublicId(publicId);
            user.setEmail(email);
            user.setTimeUpdated(System.currentTimeMillis());
            
            TUpdateUserResult result = null;
            boolean saved = false;
            try {
                result = ClientHolder.get().updateUser(user);
                saved = !Err.isFail(result.getError());
            } finally {
                if (!saved && imageChanged) {
                    ImageStorage.moveBackToTmp(ImageType.PROFILE_TYPE, newIds);
                }
            }
            if (Err.isFail(result.getError())) {
                if (Err.isNetworkError(result.getError())) {
                    _Logger.error("Network error, update user");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if (Err.isNotFound(result.getError())) {
                    _Logger.info("Notfound, update user");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "Không tìm thấy user để cập nhật");
                    return;
                }
                if (Err.isConflict(result.getError())) {
                    _Logger.info("Conflict email");
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "Email đã tồn tại");
                    return;
                }
                _Logger.error("updated user failed");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }

            if (imageChanged && oldPublicId != null && !oldPublicId.isEmpty()) {
                try {
                    ImageStorage.deleteFromFinal(ImageType.PROFILE_TYPE, oldPublicId);
                } catch (Exception e) {
                    _Logger.error("delete old user image failed: " + oldPublicId, e);
                }
            }
            if(result.getValue() != null) result.getValue().setImageUrl(ImageStorage.buildUrl(ImageType.PROFILE_TYPE, publicId));
            ok(resp, result);
        }
        catch (NumberFormatException e) {
            _Logger.info("input is incorrect format", e);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Tham số không đúng định dạng");
            return;
        } 
        catch(Exception e)
        {
            _Logger.error("profile update failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    {
        try
        {
            TUserResult ret = ClientHolder.get().getUser(getUserFromRequest(req).getUserId());
            if(Err.isFail(ret.getError()))
            {
                if(Err.isNetworkError(ret.getError()))
                {
                    _Logger.info("Network error");
                    fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                    return;
                }
                if(Err.isNotFound(ret.getError()))
                {
                    _Logger.info("Notfound error");
                    fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.FAIL, "Không tìm thấy người dùng này");
                    return;
                }
                _Logger.error("Get user failed, System error");
                fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
                return;
            }
            
            TUser result = ret.getValue();
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("userId", result.getUserId());
            data.put("phone", result.getPhone());
            data.put("email", result.getEmail());
            data.put("publicId", result.getPublicId());
            data.put("imageUrl", ImageStorage.buildUrl(ImageType.PROFILE_TYPE, result.getPublicId()));
            data.put("displayName", result.getDisplayName());
            data.put("status", result.getStatus());
            data.put("createdAt", result.getTimeCreated());
            ok(resp, data);
        }
        catch(Exception e)
        {
            _Logger.error("profile update failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}
