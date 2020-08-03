package io.renren.modules.test.utils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by zyanycall@gmail.com on 2018/6/15.
 */
public class SSH2Utils {

    Logger logger = LoggerFactory.getLogger(getClass());

    private String host;

    private String user;

    private String password;

    private int port;

    /**
     * 创建一个连接
     *
     * @param host
     *            地址
     * @param user
     *            用户名
     * @param password
     *            密码
     * @param port
     *            ssh2端口
     */
    public SSH2Utils(String host, String user, String password, int port) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.port = port;
    }

    /**
     * 上传文件，使用ssh方式
     *
     * @param filePath
     *            本地文件完整路径，若为空，表示当前路径
     * @param remotePath
     *            远程路径，若为空，表示当前路径，若服务器上无此目录，则会自动创建
     */
    public void scpPutFile(String filePath, String remotePath) {
        //文件scp到数据服务器
        Connection conn = new Connection(host, port);
        try {
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(user, password);
            if (isAuthenticated == false)
                throw new IOException("Authentication failed.文件scp到数据服务器时发生异常");
            SCPClient client = new SCPClient(conn);
            logger.error("scp文件开始 : " + filePath);
            client.put(filePath, remotePath); //本地文件scp到远程目录
//            client.get(dataServerDestDir + "00审计.zip", localDir);//远程的文件scp到本地目录
        } catch (IOException e) {
            logger.error("文件scp到数据服务器时发生异常", e);
        } finally {
            conn.close();
        }
        logger.error("scp文件结束 : " + filePath);
    }

    /**
     * 远程执行命令，返回显示结果。
     * @param command 命令
     */
    public String runCommand(String command) {
        StringBuilder returnLine = new StringBuilder();
        //跟远程服务器建立连接
        Connection conn = new Connection(host, port);
        Session sess = null;
        try {
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(user, password);
            if (isAuthenticated == false){
                throw new IOException("Authentication failed.执行命令时发生异常");
            }
            //打开一个会话session，执行linux命令
            sess = conn.openSession();
            sess.execCommand(command);

            //接收目标服务器上的控制台返回结果,输出结果。
            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

            // 仅要第一行即可
            // 多行读取对某些命令会有未知原因的卡顿
            returnLine.append(br.readLine());

            //得到脚本运行成功与否的标志 ：0－成功 非0－失败
//            logger.error("ExitCode: " + sess.getExitStatus());

            //关闭session和connection
//            sess.close();
//            conn.close();
            br.close();
        } catch (Exception e) {
            logger.error("文件scp到数据服务器时发生异常", e);
        } finally {
            if (sess != null) {
                sess.close();
            }
            conn.close();
        }
        logger.error("执行命令结束 : " + command + "\n返回值：" + returnLine);
        return returnLine.toString();
    }


    public static void main(String[] args) {

//        ArrayList<String> list = new ArrayList<>();
//        list.add("39.105.197.108");
//        list.add("47.94.167.252");
//        list.add("182.92.169.95");
//        list.add("123.57.48.191");
//        list.add("139.224.231.58");
//        list.add("106.14.63.173");
//        list.add("106.14.61.57");
//        list.add("139.224.247.6");
//        list.add("47.113.91.64");
//        list.add("47.113.113.31");
//        list.add("47.113.104.40");
//        list.add("47.113.113.197");
//        list.add("122.152.251.252");
//        list.add("106.52.112.250");
//        list.add("139.199.74.80");
//        list.add("118.89.40.13");
//        list.add("111.230.150.99");
//        list.add("203.195.224.170");
//        list.add("134.175.151.166");
//        list.add("111.231.233.52");
//        list.add("193.112.87.116");
//        list.add("123.207.251.174");
//        list.add("134.175.81.96");
//        list.add("129.204.221.126");
//        list.add("134.175.63.22");
//        list.add("134.175.153.202");
//        list.add("111.230.139.98");
//        list.add("129.28.147.204");
//        list.add("94.191.117.202");
//        list.add("94.191.125.12");
//        list.add("118.24.246.32");
//        for (String ip : list) {
//            SSH2Utils ssh2Util = new SSH2Utils(ip, "root",
//                    "yDY@28kss7AkasY", 22);
//            ssh2Util.runCommand("sed -i '/order.koolearn.com/d' /etc/hosts");
////            ssh2Util.runCommand("sed -i '$a\\172.18.142.17  s.kooup.com' /etc/hosts");
//            ssh2Util.runCommand("sed -i '$a\\140.143.215.85  order.koolearn.com' /etc/hosts");
//            System.out.println(ip + "结束！");
//        }

        ArrayList<String> list = new ArrayList<>();
        list.add("10.155.10.94");
        list.add("10.155.10.86");
        list.add("10.155.10.178");
        list.add("10.155.10.179");
        list.add("10.155.10.180");
        for (String ip : list) {
            SSH2Utils ssh2Util;
            if (ip.startsWith("10.155.10.1")) {
                ssh2Util = new SSH2Utils(ip, "zhaoyu",
                        "Dig@init5", 22);
            } else {
                ssh2Util = new SSH2Utils(ip, "zhaoyu01",
                        "root-123", 22);
            }
            ssh2Util.runCommand("sudo su root");
            ssh2Util.runCommand("sed -i '/icp.koolearn.com/d' /etc/hosts");
            ssh2Util.runCommand("sed -i '/ccp.koolearn.com/d' /etc/hosts");
//            ssh2Util.runCommand("sed -i '$a\\172.18.46.35  ccp.koolearn.com' /etc/hosts");
            ssh2Util.runCommand("sed -i '$a\\140.143.178.159 icp.koolearn.com' /etc/hosts");
            System.out.println(ip + "结束！");
        }
//        String sig = "e8f767d73873c64abbe6855919a26c80";
//        System.out.println(sig.toUpperCase());
    }
}
