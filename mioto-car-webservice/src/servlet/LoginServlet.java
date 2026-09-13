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
import util.ClientHolder;
import util.ClientInfo;
import util.CookieSigner;
import util.CookieUtil;
import util.RateLimiter;
import wrapper.CarClientWrapper;

/**
 *
 * @author tuanlee
 */
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1;
    private CarClientWrapper _mw;

    @Override
    public void init() {
        _mw = ClientHolder.get();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Map<String, Object> body = jsonBody(req);
            String phone = param(req, body, "phone");
            String pwd = param(req, body, "pwd");
            if (phone == null || phone.matches("[0-9]{9,15}")) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phone number must contain only digits and be between 9 and 15 characters long.");
                return;
            }
            if (pwd == null || pwd.length() < 6 || pwd.length() > 128) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Password must be between 6 and 128 characters long");
                return;
            }
            if (!RateLimiter.allow(phone, 5, 300) || !RateLimiter.allow(ClientInfo.ip(req), 30, 300)) {
                fail(resp, 429, Err.FAIL, "Too many request");
                return;
            }

            TLoginRequest lreq = new TLoginRequest();
            lreq.setPhone(phone);
            lreq.setPwd(pwd);
            TLoginResult ret = _mw.login(lreq, ClientInfo.from(req));

            if (Err.isFail(ret.getError())) {
                if (ret.getError() == Err.FORBIDDEN) {
                    fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN, "This account has been locked");
                } else if (Err.isNetworkError(ret.getError())) {
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Service unavailble");
                } else {
                    fail(resp, HttpServletResponse.SC_UNAUTHORIZED, Err.FAIL, "Phone or password is incorrect");
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
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "internal error");
        }

    }

}
