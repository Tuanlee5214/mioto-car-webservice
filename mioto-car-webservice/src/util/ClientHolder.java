/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import wrapper.CarClientWrapper;

/**
 *
 * @author tuanlee
 */
public class ClientHolder {
    
    private static volatile CarClientWrapper _Instance;
    private ClientHolder() {}
    private static final Object lock = new Object();
    
    public static CarClientWrapper get()
    {
        if(_Instance == null)
        {
            synchronized(lock)
            {
                if(_Instance == null)
                    _Instance = new CarClientWrapper();
            }
        }
        
        return _Instance;
    }
}
