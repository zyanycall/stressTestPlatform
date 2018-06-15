package io.renren.modules.test.utils;

import com.jcraft.jsch.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by zyanycall@gmail.com on 2018/6/15.
 */
public class SSH2Utils {

    private String host;

    private String user;

    private String password;

    private int port;

    private Session session;

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

    public void initialSession() throws Exception {
        if (session == null) {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setUserInfo(new UserInfo() {

                public String getPassphrase() {
                    return null;
                }

                public String getPassword() {
                    return null;
                }

                public boolean promptPassword(String arg0) {
                    return false;
                }

                public boolean promptPassphrase(String arg0) {
                    return false;
                }

                public boolean promptYesNo(String arg0) {
                    return true;
                }

                public void showMessage(String arg0) {
                }

            });
            session.setPassword(password);
            session.connect();
        }
    }

    /**
     * 关闭连接
     *
     * @throws Exception
     */
    public void close() throws Exception {
        if (session != null && session.isConnected()) {
            session.disconnect();
            session = null;
        }
    }

    /**
     * 上传文件
     *
     * @param localPath
     *            本地路径，若为空，表示当前路径
     * @param localFile
     *            本地文件名，若为空或是“*”，表示目前下全部文件
     * @param remotePath
     *            远程路径，若为空，表示当前路径，若服务器上无此目录，则会自动创建
     * @throws Exception
     */
    public void putFile(String localPath, String localFile, String remotePath)
            throws Exception {
        Channel channelSftp = session.openChannel("sftp");
        channelSftp.connect();
        ChannelSftp c = (ChannelSftp) channelSftp;
        String remoteFile = null;
        if (remotePath != null && remotePath.trim().length() > 0) {
            try {
                c.mkdir(remotePath);
            } catch (Exception e) {
            }
            remoteFile = remotePath + "/.";
        } else {
            remoteFile = ".";
        }
        String file = null;
        if (localFile == null || localFile.trim().length() == 0) {
            file = "*";
        } else {
            file = localFile;
        }
        if (localPath != null && localPath.trim().length() > 0) {
            if (localPath.endsWith("/")) {
                file = localPath + file;
            } else {
                file = localPath + "/" + file;
            }
        }
        c.put(file, remoteFile);

        channelSftp.disconnect();
    }

    /**
     * 上传文件
     *
     * @param filePath
     *            本地文件完整路径，若为空，表示当前路径
     * @param remotePath
     *            远程路径，若为空，表示当前路径，若服务器上无此目录，则会自动创建
     * @throws Exception
     */
    public void putFile(String filePath, String remotePath)
            throws Exception {
//        this.initialSession();
        String localFile = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
        String localPath =  filePath.substring(0, filePath.lastIndexOf(File.separator));

        putFile(localPath, localFile, remotePath);
    }

    /**
     * 远程执行命令，返回显示结果。
     * @param command 命令
     */
    public String runCommand(String command) throws Exception {
        // CommonUtil.printLogging("[" + command + "] begin", host, user);
//        this.initialSession();
        InputStream in = null;
        InputStream err = null;
        BufferedReader inReader = null;
        BufferedReader errReader = null;
        int time = 0;
        String s = null;
        boolean run = false;
        StringBuffer sb = new StringBuffer();

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(null);
        err = ((ChannelExec) channel).getErrStream();
        in = channel.getInputStream();
        channel.connect();
        inReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        errReader = new BufferedReader(new InputStreamReader(err, "UTF-8"));

        while (true) {
            s = errReader.readLine();
            if (s != null) {
                sb.append("error:" + s).append("\n");
            } else {
                run = true;
                break;
            }
        }
        while (true) {
            s = inReader.readLine();
            if (s != null) {
                sb.append("info:" + s).append("\n");
            } else {
                run = true;
                break;
            }
        }

        while (true) {
            if (channel.isClosed() || run) {
                // CommonUtil.printLogging("[" + command + "] finish: " +
                // channel.getExitStatus(), host, user);
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
            }
            if (time > 180) {
                // CommonUtil.printLogging("[" + command + "] finish2: " +
                // channel.getExitStatus(), host, user);
                break;
            }
            time++;
        }

        inReader.close();
        errReader.close();
        channel.disconnect();
//        session.disconnect();
//        System.out.println(sb.toString());
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        SSH2Utils ssh2Util = new SSH2Utils("172.16.0.170", "root","51talk", 22);
        ssh2Util.initialSession();
//        ssh2Util.putFile("D:\\E\\stressTestCases\\20180523155023932\\12121.txt", "/home/apache-jmeter-4.0\\bin\\stressTestCases");

        ssh2Util.putFile("D:\\D\\Git\\stressTestPlatform\\target", "renren-fast.war","/home/zhaoyu/apache-jmeter-4.0/bin/stressTestCases");
//        ssh2Util.runCommand("ls -l /home/zhaoyuss/renren-fast.war");
//        ssh2Util.runCommand("md5sum /home/zhaoyuss/renren-fast.wars|cut -d ' ' -f1");

        ssh2Util.close();
    }
}
