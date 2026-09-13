/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servlet;

import util.ClientHolder;
import wrapper.CarClientWrapper;

/**
 *
 * @author tuanlee
 */
public class LoginServlet extends BaseServlet{
    
    private static final long serialVersionUID = 1;
    private CarClientWrapper _mw;
    
    @Override
    public void init()
    {
        _mw = ClientHolder.get();
    }
    
    
}
