package server;


import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Minicat的主类
 * 2.0版本
 */
public class Bootstrap3 {
    /**
     * 定义Socket监听的端口号
     */
    private int port = 8080;
    private Map<String,HttpServlet> servletMap = new HashMap<>();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Minicat的启动入口
     * @param args
     */
    public static void main(String[] args) {
        Bootstrap2 bootstrap = new Bootstrap2();
        try {
            //启动Minicat
            bootstrap.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * MiniCat启动需要初始化展开的一些操作
     */
    public void start() throws Exception {
        loadServlet();


        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("========>>Minicat start on port："+port);

        while (true){
            Socket socket = serverSocket.accept();
            InputStream inputStream = socket.getInputStream();
//            //从输入流中获取请求信息
//            int count = 0 ;
//            while (count==0){
//                count = inputStream.available();
//            }
//            byte[] bytes = new byte[count];
//            inputStream.read(bytes);
//            System.out.println("请求信息=====>>"+new String(bytes));

            //封装Resuest对象和Response对象
            Request request = new Request(inputStream);
            Response response = new Response(socket.getOutputStream());

            String url = request.getUrl();

            if (servletMap.get(url) == null){
                response.outputHtml(request.getUrl());
            }else{
                HttpServlet httpServlet = servletMap.get(url);
                httpServlet.service(request,response);
            }


            socket.close();
        }

    }

    private void loadServlet(){
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("web.xml");
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            //根元素
            Element rootElement = document.getRootElement();
            /**
             * 1, 找到所有的servlet标签，找到servlet-name和servlet-class
             * 2, 根据servlet-name找到<servlet-mapping>中与其匹配的<url-pattern>
             */
            List<Element> selectNodes = rootElement.selectNodes("//servlet");
            for (int i = 0; i < selectNodes.size(); i++) {
                Element element = selectNodes.get(i);
                /**
                 * 1, 找到所有的servlet标签，找到servlet-name和servlet-class
                 */
                //<servlet-name>dxh</servlet-name>
                Element servletNameElement =(Element)element.selectSingleNode("servlet-name");
                String servletName = servletNameElement.getStringValue();
                //<servlet-class>server.DxhServlet</servlet-class>
                Element servletClassElement =(Element)element.selectSingleNode("servlet-class");
                String servletClass = servletClassElement.getStringValue();
                /**
                 * 2, 根据servlet-name找到<servlet-mapping>中与其匹配的<url-pattern>
                 */
                //Xpath表达式：从/web-app/servlet-mapping下查询，查询出servlet-name=servletName的元素
                Element servletMapping =(Element)rootElement.selectSingleNode("/web-app/servlet-mapping[servlet-name='" + servletName + "']'");
                // /dxh
                String urlPattern = servletMapping.selectSingleNode("url-pattern").getStringValue();
                servletMap.put(urlPattern,(HttpServlet) Class.forName(servletClass).newInstance());
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



}
