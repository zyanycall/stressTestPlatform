package io.renren.modules.test.utils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by zyanycall@gmail.com on 2018/6/15.
 */
public class SSH2Utils {

    Logger logger = LoggerFactory.getLogger(getClass());

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

    public Session initialSession() throws JSchException {
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
            session.setTimeout(30000);
            session.connect();
        }
        return session;
    }

    /**
     * 关闭连接
     *
     */
    public void close() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        session = null;
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
            throws JSchException, SftpException {
        Channel channelSftp = session.openChannel("sftp");
        channelSftp.connect();
        ChannelSftp sftp = (ChannelSftp) channelSftp;
        String remoteFile = null;
        if (remotePath != null && remotePath.trim().length() > 0) {
            try {
                sftp.mkdir(remotePath);
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
        sftp.put(file, remoteFile);

        channelSftp.disconnect();
        channelSftp = null;
    }

    /**
     * 上传文件，使用sftp方式
     *
     * @param filePath
     *            本地文件完整路径，若为空，表示当前路径
     * @param remotePath
     *            远程路径，若为空，表示当前路径，若服务器上无此目录，则会自动创建
     * @throws Exception
     */
    public void putFile(String filePath, String remotePath)
            throws SftpException, JSchException {
//        this.initialSession();
        String localFile = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
        String localPath =  filePath.substring(0, filePath.lastIndexOf(File.separator));

        putFile(localPath, localFile, remotePath);
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
        Connection conn = new Connection(host);
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
    public String runCommand(String command) throws JSchException, IOException {
        // CommonUtil.printLogging("[" + command + "] begin", host, user);
//        this.initialSession();
        InputStream in = null;
//        InputStream err = null;
        BufferedReader inReader = null;
//        BufferedReader errReader = null;
        int time = 0;
        String s = null;
        boolean run = false;
        StringBuffer sb = new StringBuffer();

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(null);
//        err = ((ChannelExec) channel).getErrStream();
        in = channel.getInputStream();
        channel.connect();
        inReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
//        errReader = new BufferedReader(new InputStreamReader(err, "UTF-8"));

//        while (true) {
//            s = errReader.readLine();
//            if (s != null) {
//                sb.append("error:" + s).append("\n");
//                run = true;
//                break;
//            } else {
//                run = true;
//                break;
//            }
//        }
        while (true) {
            s = inReader.readLine();
            if (s != null) {
                sb.append(s);
                run = true;
                break;
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
//        errReader.close();
        inReader = null;
        channel.disconnect();
        channel = null;
        return sb.toString();
    }
}
