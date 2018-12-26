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
        //文件scp到数据服务器
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
            sess.close();
            conn.close();
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
}
