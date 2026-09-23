/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author tuanlee
 */
public class HealthServlet extends BaseServlet{
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    {
        ok(resp, "OK");
    }
}
    