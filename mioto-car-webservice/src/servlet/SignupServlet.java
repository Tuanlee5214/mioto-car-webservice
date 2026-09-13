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
import util.ClientHolder;
import util.ClientInfo;
import util.CookieSigner;
import util.CookieUtil;
import wrapper.CarClientWrapper;

/**
 *
 * @author tuanlee
 */
public class SignupServlet extends BaseServlet {

    private static final long serialVersionUID = 1;
    private CarClientWrapper _mw;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @Override
    public void init() {
        _mw = ClientHolder.get();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try 
        {
            Map<String, Object> body = jsonBody(req);
            String phone = param(req, body, "phone");
            String pwd = param(req, body, "pwd");
            String email = param(req, body, "email");
            String name = param(req, body, "displayName");

            if (phone == null || phone.matches("[0-9]{9,15}")) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Phone number must contain only digits and be between 9 and 15 characters long.");
                return;
            }
            if (pwd == null || pwd.length() < 6 || pwd.length() > 128) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Password must be between 6 and 128 characters long");
                return;
            }
            if (name == null || name.isEmpty()) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Name is not blank");
                return;
            }
            if (email == null || email.isEmpty()) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Email is not blank");
                return;
            }
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            if (!matcher.matches()) {
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Email is not in the correct format");
                return;
            }
            TSignUpRequest sureq = new TSignUpRequest();
            sureq.setPhone(phone);
            sureq.setDisplayName(name);
            sureq.setEmail(email);
            sureq.setPwd(pwd);

            TLoginResult ret = _mw.signup(sureq, ClientInfo.from(req));

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
        }
        catch(Exception e)
        {
            _Logger.error("signup failed", e);           
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL,"internal error");
        }
    }
            
}
