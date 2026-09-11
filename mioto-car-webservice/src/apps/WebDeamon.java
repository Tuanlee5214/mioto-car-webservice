/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package apps;

/**
 *
 * @author tuanlee
 */
public class WebDeamon {
    public static void main(String[] args) throws Exception {
        String env  = System.getProperty("appenv", "development");
        String conf = System.getProperty("conf", "conf");
        org.apache.log4j.PropertyConfigurator.configure(
                conf + java.io.File.separator + env + ".log4j.ini");

        final HttpServer server = new HttpServer();
        if (server.start() < 0) {
            System.exit(503);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stop();
            }
        });
    }
}
