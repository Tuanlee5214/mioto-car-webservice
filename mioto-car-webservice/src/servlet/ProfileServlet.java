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
import thrift.TUpdateUserResult;
import thrift.TUser;
import thrift.TUserResult;
import util.ClientHolder;

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
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Name is not blank");
                return;
            }
            Matcher macher = EMAIL_PATTERN.matcher(email);
            if(!macher.matches()){
                _Logger.info("Email is incorrect format, email = " + email);
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.BAD_REQUEST, "Email is not in the correct format");
                return;
            }
            
            TUser user = new TUser();
            user.setUserId(getUserFromRequest(req).getUserId());
            user.setDisplayName(name);
            user.setEmail(email);
            user.setTimeUpdated(System.currentTimeMillis());
            TUpdateUserResult ret = ClientHolder.get().updateUser(user);
            
            if(Err.isFail(ret.getError()))
            {
                _Logger.info("Update failed");
                fail(resp, HttpServletResponse.SC_BAD_REQUEST, Err.FAIL, "Can not update");
                return;
            }
            
            ok(resp, ret.value);
        }
        catch(Exception e)
        {
            _Logger.error("profile update failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "internal error");
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
                _Logger.info("User not found");
                fail(resp, HttpServletResponse.SC_NOT_FOUND, Err.NOT_FOUND, "User not found");
                return;
            }
            
            TUser result = ret.getValue();
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("userId", result.getUserId());
            data.put("phone", result.getPhone());
            data.put("email", result.getEmail());
            data.put("displayName", result.getDisplayName());
            data.put("status", result.getStatus());
            ok(resp, data);
        }
        catch(Exception e)
        {
            _Logger.error("profile update failed", e);
            fail(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Err.FAIL, "internal error");
        }
    }
}
