package io.renren.modules.test.handler;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * 自定义钩子Handler，用于异步操作入库及日志打印等。
 * 集成父类方式实现
 * Created by zyanycall@gmail.com on 15:40.
 */
public class FileResultHandler extends DefaultExecuteResultHandler {
    Logger logger = LoggerFactory.getLogger(getClass());

    // 命令正常输出
    ByteArrayOutputStream outputStream;
    // 命令错误输出
    ByteArrayOutputStream errorStream;

    // 构造函数
    public FileResultHandler(ByteArrayOutputStream outputStream, ByteArrayOutputStream errorStream) {
        super();
        this.outputStream = outputStream;
        this.errorStream = errorStream;
    }

    /**
     * jmx脚本执行成功会走到这里
     * 重写父类方法，增加入库及日志打印
     */
    @Override
    public void onProcessComplete(final int exitValue) {
        super.onProcessComplete(exitValue);
        try {
            logger.error(outputStream.toString("GBK"));
        } catch (UnsupportedEncodingException e) {
            logger.error("打印执行结果内容失败", e);
        }
    }

    /**
     * jmx脚本执行失败会走到这里
     * 重写父类方法，增加入库及日志打印
     */
    @Override
    public void onProcessFailed(final ExecuteException e) {
        super.onProcessFailed(e);
        logger.error("启动Jmeter执行脚本失败", e);
        try {
            logger.error(errorStream.toString("GBK"));
        } catch (UnsupportedEncodingException e1) {
            logger.error("打印执行结果内容失败", e1);
        }
    }
}
