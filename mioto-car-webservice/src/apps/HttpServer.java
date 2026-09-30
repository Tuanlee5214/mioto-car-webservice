    /*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package apps;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import servlet.CarBrandServlet;
import servlet.DistrictServlet;
import servlet.FeatureServlet;
import servlet.FeePolicyServlet;
import servlet.FeedBackServlet;
import servlet.HealthServlet;
import servlet.LoginServlet;
import servlet.LogoutServlet;
import servlet.MeServlet;
import servlet.ProfileServlet;
import servlet.ProvinceServlet;
import servlet.SignupServlet;
import servlet.VoucherServlet;
import util.Config;
    
/**
 *
 * @author tuanlee
 */
public class HttpServer {
    private static final Logger _Logger = Logger.getLogger(HttpServer.class);

    private final int _port;
    private final Server _server;

    public HttpServer() {
        _port = Config.getInt(HttpServer.class, "web", "port", 8081);
        _server = new Server(_port);

        // 1. servlets, registered in CODE - no web.xml anywhere in this project
        ServletHandler servlets = new ServletHandler();
        servlets.addServletWithMapping(HealthServlet.class,  "/health");
        servlets.addServletWithMapping(SignupServlet.class,  "/api/signup");
        servlets.addServletWithMapping(LoginServlet.class,   "/api/login");
        servlets.addServletWithMapping(LogoutServlet.class,  "/api/logout");
        servlets.addServletWithMapping(ProfileServlet.class, "/api/profiles");
        servlets.addServletWithMapping(MeServlet.class, "/api/checkauth");
        servlets.addServletWithMapping(CarBrandServlet.class, "/api/car-brands/*");
        servlets.addServletWithMapping(DistrictServlet.class, "/api/districts/*");
        servlets.addServletWithMapping(FeatureServlet.class, "/api/features/*");
        servlets.addServletWithMapping(FeePolicyServlet.class, "/api/fee-policies/*");
        servlets.addServletWithMapping(FeedBackServlet.class, "/api/feedbacks/*");
        servlets.addServletWithMapping(ProvinceServlet.class, "/api/provinces/*");
        servlets.addServletWithMapping(VoucherServlet.class, "/api/vouchers/*");
        
        // 2. static files from ./public served at /static
        ContextHandler statics = new ContextHandler("/static");
        statics.setResourceBase(Config.getString(HttpServer.class, "web", "resource", "./public"));
        statics.setHandler(new ResourceHandler());

        // 3. try the static handler first, then the servlets
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new org.eclipse.jetty.server.Handler[] { statics, servlets });
        _server.setHandler(handlers);
        _server.setStopAtShutdown(true);
    }

    public int start() {
        try {
            _server.start();
            _Logger.info("web listening on http://127.0.0.1:" + _port);
            return 0;
        } catch (Exception ex) {
            _Logger.error("cannot start web on port " + _port, ex);
            return -1;
        }
    }

    public void stop() {
        try {
            _server.stop();
            _server.join();
            _Logger.info("web stopped");
        } catch (Exception ex) {
            _Logger.error("error stopping web", ex);
        }
    }
}
