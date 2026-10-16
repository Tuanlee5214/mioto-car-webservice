/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberStrategy;
import com.google.gson.stream.JsonReader;
import error.Err;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import static servlet.AuthServlet.ATTR_USER;
import thrift.TLoginInfo;
import thrift.TRole;
import thrift.TUser;
import thrift.TUserResult;
import thrift.TUserRoleResult;
import util.ClientHolder;
import util.ClientInfo;
import util.CookieSigner;
import util.CookieUtil;

/**
 *
 * @author tuanlee
 */
public class BaseServlet extends HttpServlet{
    private static final long serialVersionUID = 1L;
    public static final String ATTR_USER = "auth.user";
    public static final String ATTR_SESSION_ID = "auth.sessionId";

    protected static final Logger _Logger = Logger.getLogger(BaseServlet.class);
    protected static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                String value = in.nextString();
                try {
                    long valueLong = Long.parseLong(value);
                    if (valueLong >= Integer.MIN_VALUE && valueLong <= Integer.MAX_VALUE) {
                        return (int) valueLong;
                    }
                    return valueLong; 
                } catch (NumberFormatException e) {
                    return Double.parseDouble(value);
                }
            }
        })
        .create();


    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");       
        resp.setCharacterEncoding("UTF-8");
        
        this.setCorsHeader(resp);
        if("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }            
        
        super.service(req, resp);
    }

    protected String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    protected String param(HttpServletRequest req, Map<String, Object> body, String name) {
        String v = req.getParameter(name);
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        if (body != null && body.get(name) != null) {
            return String.valueOf(body.get(name)).trim();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> jsonBody(HttpServletRequest req) {
        try {
            String raw = readBody(req);
            if (raw.isEmpty()) {
                return null;
            }
            return GSON.fromJson(raw, Map.class);
        } catch (Exception ex) {
            return null;                         
        }
    }

    protected void writeJson(HttpServletResponse resp, int status, Object body) {
        try {
            resp.setStatus(status);               
            resp.setContentType("application/json");
            resp.getWriter().write(GSON.toJson(body));   
        } catch (IOException ex) {
            _Logger.error("cannot write response", ex);
        }
    }

    protected Map<String, Object> envelope(int error, Object data) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("error", error);
        body.put("data", data);
        return body;
    }

    protected void ok(HttpServletResponse resp, Object data) {
        writeJson(resp, HttpServletResponse.SC_OK, envelope(0, data));
    }

    protected void fail(HttpServletResponse resp, int httpStatus, int error, String message) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("error", error);
        body.put("message", message);             
        writeJson(resp, httpStatus, body);
    }
        
    protected TUserResult getAuthenticatedUser(HttpServletRequest req, HttpServletResponse resp)
    {
        TUserResult result = new TUserResult();
        long sessionId = CookieSigner.verify(CookieUtil.read(req));
        if(sessionId <= 0) return new TUserResult(Err.UNAUTHORIZED, "");
        TLoginInfo info = ClientInfo.getLoginInfo(req);
        
        TUserResult ret = ClientHolder.get().getUserBySessionId(sessionId, info);
        if(Err.isFail(ret.getError()) || ret.value == null)
        {
            if(Err.isNetworkError(ret.getError()))
            {
                _Logger.error("session lookup failed, err=" + ret.getError());
                return new TUserResult(Err.NO_CONNECTION, "");
            }
            else 
            {
                CookieUtil.clear(resp);
                return new TUserResult(Err.UNAUTHORIZED, "");
            }
        }
        
        result.setError(Err.SUCCESS);
        result.setValue(ret.value);
        return result;
    }
    
    protected void setCorsHeader(HttpServletResponse resp)
    {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    
    protected Integer parseIdFromRequest(HttpServletRequest req)
    {
        String ret = req.getPathInfo();
        if(ret == null || "/".equals(ret)) 
        {
            return 0;
        }
        try {
            int id = Integer.parseInt(ret.substring(1));
            return id > 0 ? id : null;
        } 
        catch (NumberFormatException e) {
            return null;
        }
    }
        protected TUser getUserFromRequest(HttpServletRequest req) {
        TUser v = (TUser) req.getAttribute(ATTR_USER);
        return v == null ? null : new TUser(v);
    }
    
    protected boolean hasRole(HttpServletRequest req, HttpServletResponse resp, String role)
    {
        TUser user = getUserFromRequest(req);
        if(user != null)
        {
            TUserRoleResult result = ClientHolder.get().getUserRole(user.getUserId());
            if(Err.isNetworkError(result.getError()))
            {
                _Logger.error("Network error, check role " + role);
                fail(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, Err.FAIL, "Lỗi kết nối mạng");
                return false;
            }
            
            if(result.getValue() != null && result.getValue().isIsSuperAdmin()) return true;
            
            if(Err.isSuccess(result.getError()) && result.getValue() != null)
            {
                List<TRole> roles = result.getValue().getRoles() == null ? null : result.getValue().getRoles();
                if(roles != null)
                {
                    for (TRole r : roles) {
                        if (role.equals(r.getName())) {
                            return true;
                        }
                    }
                }
            }
        }
        _Logger.info("Forbidden, userId=" + (user == null ? "unknown" : user.getUserId())
                + ", " + req.getMethod() + " " + req.getRequestURI());
        fail(resp, HttpServletResponse.SC_FORBIDDEN, Err.FORBIDDEN,
                "Bạn không có quyền thực hiện hành động này");
        return false;
    }
    
    protected boolean requireAuth(HttpServletRequest req) {
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            return true;
        }
        String path = req.getPathInfo();
        return "/mine".equals(path) || "/admin".equals(path);
    }
}
