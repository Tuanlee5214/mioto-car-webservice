package servlet;

import error.Err;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import thrift.TUser;
import util.ImageStorage;
import util.RateLimiter;

public class ImageUploadServlet extends AuthServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            TUser user = getUserFromRequest(req);
            if (!RateLimiter.allow("upload:" + user.getUserId(), 20, 60)) {
                fail(resp, 429, Err.BAD_REQUEST, "Thao tác quá nhanh, thử lại sau");
                return;
            }

            InputStream in = req.getInputStream();
            String imageId = ImageStorage.saveToTmp(in, user.getUserId());

            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("publicId", imageId);
            ok(resp, data);
        } catch (IllegalArgumentException e) {
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            _Logger.error("Upload image failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }
    }
}