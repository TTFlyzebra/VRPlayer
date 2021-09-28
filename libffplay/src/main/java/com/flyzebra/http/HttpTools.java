package com.flyzebra.http;


import com.flyzebra.utils.FlyLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @hide
 * ClassName: HttpTools
 * Description:
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 20-1-8 下午2:27
 */
public class HttpTools {
    public static int CONNECT_TIME_OUT = 5000;
    public static int READ_TIME_OUT = 5000;
    /**
     * 向指定URL发送GET方法的请求
     *
     * @param httpurl
     *            请求参数用?拼接在url后边，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return result 所代表远程资源的响应结果
     */
    public static HttpResult doGet(String httpurl) {
        HttpURLConnection connection = null;
        InputStream is = null;
        BufferedReader br = null;
        HttpResult result = new HttpResult();// 返回结果字符串
        try {
            // 创建远程url连接对象
            URL url = new URL(httpurl);
            // 通过远程url连接对象打开一个连接，强转成httpURLConnection类
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接方式：get
            connection.setRequestMethod("GET");
            // 设置连接主机服务器的超时时间：15000毫秒
            connection.setConnectTimeout(CONNECT_TIME_OUT);
            // 设置读取远程返回的数据时间：60000毫秒
            connection.setReadTimeout(READ_TIME_OUT);
            // 发送请求
            connection.connect();
            // 通过connection连接，获取输入流
            if (200 == (result.code = connection.getResponseCode())) {
                is = connection.getInputStream();
                // 封装输入流is，并指定字符集
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                // 存放数据
                StringBuffer sbf = new StringBuffer();
                String temp = null;
                while ((temp = br.readLine()) != null) {
                    sbf.append(temp).append("\r\n");
                }
                result.data = sbf.toString();
            }
        } catch (MalformedURLException e) {
            FlyLog.e(e.toString());
        } catch (IOException e) {
            FlyLog.e(e.toString());
        } finally {
            // 关闭资源
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    FlyLog.e(e.toString());
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    FlyLog.e(e.toString());
                }
            }
            if(connection!=null){
                connection.disconnect();// 关闭远程连接
            }
        }
        return result;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param httpUrl
     *            发送请求的 URL
     * @param param
     *            请求参数应该是{"key":"==g43sEvsUcbcunFv3mHkIzlHO4iiUIT R7WwXuSVKTK0yugJnZSlr6qNbxsL8OqCUAFyCDCoRKQ882m6cTTi0q9uCJsq JJvxS+8mZVRP/7lWfEVt8/N9mKplUA68SWJEPSXyz4MDeFam766KEyvqZ99d"}的形式。
     * @return 所代表远程资源的响应结果
     */
    public static HttpResult doPostJson(String httpUrl, String param) {
        FlyLog.d("post url=%s",httpUrl);
        FlyLog.d("post param=%s",param);
        HttpURLConnection connection = null;
        InputStream is = null;
        OutputStream os = null;
        BufferedReader br = null;
        HttpResult result = new HttpResult();
        try {
            URL url = new URL(httpUrl);
            // 通过远程url连接对象打开连接
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接请求方式
            connection.setRequestMethod("POST");
            // 设置连接主机服务器超时时间：15000毫秒
            connection.setConnectTimeout(CONNECT_TIME_OUT);
            // 设置读取主机服务器返回数据超时时间：60000毫秒
            connection.setReadTimeout(READ_TIME_OUT);
            // 默认值为：false，当向远程服务器传送数据/写数据时，需要设置为true
            connection.setDoOutput(true);
            // 默认值为：true，当前向远程服务读取数据时，设置为true，该参数可有可无
            connection.setDoInput(true);
            // 设置传入参数的格式:请求参数应该是 name1=value1&name2=value2 的形式。
            connection.setRequestProperty("Content-Type", "application/json");
            // 设置鉴权信息：Authorization: Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0
            //connection.setRequestProperty("Authorization", "Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0");
            // 通过连接对象获取一个输出流
            os = connection.getOutputStream();
            // 通过输出流对象将参数写出去/传输出去,它是通过字节数组写出的
            os.write(param.getBytes());
            // 通过连接对象获取一个输入流，向远程读取
            result.code = connection.getResponseCode();
            if (200 == (result.code = connection.getResponseCode())) {
                is = connection.getInputStream();
                // 对输入流对象进行包装:charset根据工作项目组的要求来设置
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuffer sbf = new StringBuffer();
                String temp = null;
                // 循环遍历一行一行读取数据
                while ((temp = br.readLine()) != null) {
                    sbf.append(temp).append("\r\n");
                }
                result.data = sbf.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 断开与远程地址url的连接
            if(connection!=null){
                connection.disconnect();
            }
        }
        return result;
    }

    /**
     *
     * @param httpUrl  请求的url
     * @param param  form表单的参数（key,value形式）
     * @return
     */
    public static HttpResult doPostForm(String httpUrl, Map param) {

        HttpURLConnection connection = null;
        InputStream is = null;
        OutputStream os = null;
        BufferedReader br = null;
        HttpResult result = new HttpResult();
        try {
            URL url = new URL(httpUrl);
            // 通过远程url连接对象打开连接
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接请求方式
            connection.setRequestMethod("POST");
            // 设置连接主机服务器超时时间：15000毫秒
            connection.setConnectTimeout(CONNECT_TIME_OUT);
            // 设置读取主机服务器返回数据超时时间：60000毫秒
            connection.setReadTimeout(READ_TIME_OUT);
            // 默认值为：false，当向远程服务器传送数据/写数据时，需要设置为true
            connection.setDoOutput(true);
            // 默认值为：true，当前向远程服务读取数据时，设置为true，该参数可有可无
            connection.setDoInput(true);
            // 设置传入参数的格式:请求参数应该是 name1=value1&name2=value2 的形式。
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            // 设置鉴权信息：Authorization: Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0
            //connection.setRequestProperty("Authorization", "Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0");
            // 通过连接对象获取一个输出流
            os = connection.getOutputStream();
            // 通过输出流对象将参数写出去/传输出去,它是通过字节数组写出的(form表单形式的参数实质也是key,value值的拼接，类似于get请求参数的拼接)
            os.write(createLinkString(param).getBytes());
            // 通过连接对象获取一个输入流，向远程读取
            if (200 == (result.code = connection.getResponseCode())) {
                is = connection.getInputStream();
                // 对输入流对象进行包装:charset根据工作项目组的要求来设置
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuffer sbf = new StringBuffer();
                String temp = null;
                // 循环遍历一行一行读取数据
                while ((temp = br.readLine()) != null) {
                    sbf.append(temp).append("\r\n");
                }
                result.data = sbf.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 断开与远程地址url的连接
            if(connection!=null){
                connection.disconnect();
            }
        }
        return result;
    }
    /**
     * 把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
     * @param params 需要排序并参与字符拼接的参数组
     * @return 拼接后字符串
     */
    private static String createLinkString(Map<String, String> params) {

        List<String> keys = new ArrayList<String>(params.keySet());
        Collections.sort(keys);

        StringBuilder prestr = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
                prestr.append(key).append("=").append(value);
            } else {
                prestr.append(key).append("=").append(value).append("&");
            }
        }

        return prestr.toString();
    }


}