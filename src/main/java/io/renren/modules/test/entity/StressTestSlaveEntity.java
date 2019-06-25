package io.renren.modules.test.entity;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by zyanycall@gmail.com on 15:24.
 */
public class StressTestSlaveEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long slaveId;

    /**
     * 节点名称
     */
    @NotBlank(message="节点名称不能为空")
    private String slaveName;

    /**
     * IP地址
     */
    @NotBlank(message="IP地址不能为空")
    @Pattern(regexp = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")
    private String ip;

    /**
     * 端口号
     */
    @NotBlank(message="Jmeter端口号不能为空")
    @Min(value = 0)
    private String JmeterPort;

    /**
     * 端口号
     */
    @NotBlank(message="ssh端口号不能为空")
    @Min(value = 0)
    private String sshPort;

    private String userName;

    private String passwd;

    /**
     * 子节点的Jmeter路径
     */
    private String homeDir;

    /**
     * 状态  0：禁用  1：正常
     */
    private Integer status;

    /**
     * 分布式节点机权重
     */
    @Min(value = 1)
    private String weight;

    /**
     * 提交的用户
     */
    private String addBy;

    /**
     * 修改的用户
     */
    private String updateBy;

    /**
     * 提交的时间
     */
    private Date addTime;

    /**
     * 更新的时间
     */
    private Date updateTime;

    public Long getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(Long slaveId) {
        this.slaveId = slaveId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getJmeterPort() {
        return JmeterPort;
    }

    public void setJmeterPort(String jmeterPort) {
        JmeterPort = jmeterPort;
    }

    public String getSshPort() {
        return sshPort;
    }

    public void setSshPort(String sshPort) {
        this.sshPort = sshPort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAddBy() {
        return addBy;
    }

    public void setAddBy(String addBy) {
        this.addBy = addBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getSlaveName() {
        return slaveName;
    }

    public void setSlaveName(String slaveName) {
        this.slaveName = slaveName;
    }

    public String getHomeDir() {
        return homeDir;
    }

    public void setHomeDir(String homeDir) {
        this.homeDir = homeDir;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    // 权重为0,100,为空，跳过去不做处理。
    public String getWeight() {
        return weight == null ? "100" : weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }
}
