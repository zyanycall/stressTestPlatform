package io.renren.modules.test.entity;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Min;
import java.io.File;
import java.io.Serializable;
import java.util.Date;

/**
 * 性能测试报告
 */
public class StressTestReportsEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long reportId;

    /**
     * 用例ID
     */
    @Min(value= 1)
    private Long caseId;

    /**
     * 文件ID
     */
    @Min(value= 1)
    private Long fileId;

    /**
     * 测试用例文件名
     */
    @NotBlank(message="用例名不能为空")
    private String originName;

    /**
     * 测试报告保存文件名，唯一化
     */
    private String reportName;

    /**
     * 测试结果文件大小
     */
    private Long fileSize = 0L;

    /**
     * 任务状态  0：初始状态  1：正在运行  2：成功执行  3：运行出现异常
     */
    private Integer status;

    /**
     * 描述
     */
    private String remark;

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

    /**
     * 性能测试结果文件csv
     */
    private File file;

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
