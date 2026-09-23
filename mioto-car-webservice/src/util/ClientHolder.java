/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import org.apache.log4j.Logger;
import wrapper.CarClientWrapper;

/**
 *
 * @author tuanlee
 */
public class ClientHolder {
    
    private static final Logger _Logger = Logger.getLogger(ClientHolder.class);
    private static volatile CarClientWrapper _Instance;
    private ClientHolder() {}
    private static final Object lock = new Object();
    private static final String _host = Config.getString(CarClientWrapper.class, "mw", "host", "127.0.0.1");
    private static final int _port = Config.getInt(CarClientWrapper.class, "mw", "port", 10101);
    private static final int _timeout = Config.getInt(CarClientWrapper.class, "mw", "timeout", 3000);
    private static final String _source = Config.getString(CarClientWrapper.class, "mw", "source", "mioto-car-web");
    
    public static CarClientWrapper get()
    {
        if(_Instance == null)
        {
            synchronized(lock)
            {
                if(_Instance == null)
                {
                    _Logger.info("Initializing CarClientWrapper -> host=" + _host + " port=" + _port + " timeout=" + _timeout);
                    _Instance = new CarClientWrapper(_host, _port, _timeout, _source);
                }
            }
        }
        _Logger.info("Initializing CarClientWrapper -> host=" + _host + " port=" + _port + " timeout=" + _timeout);
        return _Instance;
    }
}
