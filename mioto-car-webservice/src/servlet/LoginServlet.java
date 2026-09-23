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
import thrift.TLoginRequest;
import thrift.TLoginResult;
import thrift.TUserResult;
import util.ClientHolder;
import util.ClientInfo;
import util.CookieSigner;
import util.CookieUtil;
import util.RateLimiter;

/**
 *
 * @author tuanlee
 */
public class LoginServlet extends BaseServlet {
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            
            TUserResult result = getAuthenticatedUser(req, resp);
            if (Err.isNetworkError(result.getError())) {
                _Logger.error("session lookup failed, err=" + result.getError());
                fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, result.getError(), "Lỗi server");
                return;
            }
            else if(Err.isSuccess(result.getError())) 
            {
                fail(resp, HttpServletResponse.SC_OK, Err.FAIL, "Bạn đã đăng nhập rồi.");
                return;
            }
            Map<String, Object> body = jsonBody(req);
            String phone = param(req, body, "phone");
            String pwd = param(req, body, "pwd");
            if (phone == null || !phone.matches("[0-9]{9,15}")) {
                _Logger.info("Phone is incorrect format, phone = " + phone);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Số điện thoại chỉ được chứa chữ số và có từ 9 tới 15 kí tự");
                return;
            }
            if (pwd == null || pwd.length() < 6 || pwd.length() > 128) {
                _Logger.info("Password is incorrect format");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Mật khẩu phải có từ 6 tới 128 kí tự");
                return;
            }
            if (!RateLimiter.allow(phone, 5, 300) || !RateLimiter.allow(ClientInfo.ip(req), 30, 300)) {
                _Logger.warn("Too many request");
                fail(resp, 429, Err.FAIL, "Bạn đã gửi quá nhiều yêu cầu vui lòng thử lại sau");
                return;
            }

            TLoginRequest lreq = new TLoginRequest();
            lreq.setPhone(phone);
            lreq.setPwd(pwd);
            TLoginResult ret = ClientHolder.get().login(lreq, ClientInfo.from(req));

            if (Err.isFail(ret.getError())) {
                if (ret.getError() == Err.FORBIDDEN) {
                    _Logger.info("Locked account, phone = " + phone);
                    fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN, "Tài khoản này đã bị khóa");
                } else if (Err.isNetworkError(ret.getError())) {
                    _Logger.error("Service error");
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi server");
                } else {
                    _Logger.info("Phone or password is incorrect phone= " + phone);
                    fail(resp, HttpServletResponse.SC_UNAUTHORIZED, Err.FAIL, "Tài khoản hoặc mật khẩu không đúng");
                }
                return;
            }

            CookieUtil.set(resp, CookieSigner.sign(ret.getSessionId()));

            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("user", ret.getUser());
            data.put("time-expired", ret.getTimeExpired());
            ok(resp, data);

        } catch (Exception e) {
            _Logger.error("login failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Lỗi hệ thống");
        }

    }

}
