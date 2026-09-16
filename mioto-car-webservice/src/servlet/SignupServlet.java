/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import error.Err;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import thrift.TLoginResult;
import thrift.TSignUpRequest;
import thrift.TUserResult;
import util.ClientHolder;
import util.ClientInfo;
import util.CookieSigner;
import util.CookieUtil;

/**
 *
 * @author tuanlee
 */
public class SignupServlet extends BaseServlet {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            TUserResult result = getAuthenticatedUser(req, resp);
            if (Err.isNetworkError(result.getError())) {
                _Logger.error("session lookup failed, err=" + result.getError());
                fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, result.getError(), "service unavailable");
                return;
            }
            else if(Err.isSuccess(result.getError())) 
            {
                _Logger.info("User has already logged in, userId = " + String.valueOf(result.value.getUserId()));
                fail(resp, HttpServletResponse.SC_CONFLICT, Err.FAIL, "You are currently logged in. Please log out if you want to create a new account");
                return;
            }

            Map<String, Object> body = jsonBody(req);
            String phone = param(req, body, "phone");
            String pwd = param(req, body, "pwd");
            String email = param(req, body, "email");
            String name = param(req, body, "displayName");

            if (phone == null || !phone.matches("[0-9]{9,15}")) {
                _Logger.info("Phone is incorrect format");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phone number must contain only digits and be between 9 and 15 characters long.");
                return;
            }
            if (pwd == null || pwd.length() < 6 || pwd.length() > 128) {
                _Logger.info("Password is not in range (6 -128) characters");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Password must be between 6 and 128 characters long");
                return;
            }
            if (name == null || name.isEmpty()) {
                _Logger.info("Name is blank");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Name is not blank");
                return;
            }
            if (email == null || email.isEmpty()) {
                _Logger.info("Email is blank");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Email is not blank");
                return;
            }
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            if (!matcher.matches()) {
                _Logger.info("Email is incorrect format, email = " + email);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Email is not in the correct format");
                return;
            }
            TSignUpRequest sureq = new TSignUpRequest();
            sureq.setPhone(phone);
            sureq.setDisplayName(name);
            sureq.setEmail(email);
            sureq.setPwd(pwd);

            _Logger.info("Call signup from mw");
            TLoginResult ret = ClientHolder.get().signup(sureq, ClientInfo.from(req));

            if (Err.isFail(ret.getError())) {
                if (ret.getError() == Err.CONFLICT) {
                    fail(resp, HttpServletResponse.SC_CONFLICT, Err.CONFLICT, "This phone is already registered");
                } else if (Err.isNetworkError(ret.getError())) {
                    fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "Service unavailable");
                }
                return;
            }

            CookieUtil.set(resp, CookieSigner.sign(ret.getSessionId()));
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("user", ret.getUser());
            data.put("time-expired", ret.getTimeExpired());
            writeJson(resp, HttpServletResponse.SC_CREATED, envelope(0, data));
        } catch (Exception e) {
            _Logger.error("signup failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "internal error");
        }
    }

}
