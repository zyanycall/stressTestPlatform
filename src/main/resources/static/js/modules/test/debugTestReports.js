$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'test/debugReports/list',
        datatype: "json",
        colModel: [
            {label: '报告ID', name: 'reportId', width: 30, key: true},
            {
                label: '报告名称', name: 'originName', width: 70, sortable: false, formatter: function (value, options, row) {
                if (row.status === 2) {
                    var reportDir = row.reportName.substring(0, row.reportName.lastIndexOf("."));
                    return "<a href='" + baseURL + "testReport/" + reportDir + ".html'>" + value + "</a>";
                } else {
                    return value;
                }
            }
            },
            {label: '描述', name: 'remark', width: 65, sortable: false},
            {label: '脚本ID', name: 'fileId', width: 30},
            {
                label: '结果文件大小', name: 'fileSize', width: 45, formatter: function (value, options, row) {
                return conver(value);
            }
            },
            {label: '添加时间', name: 'addTime', width: 60},
            {
                label: '状态', name: 'status', width: 35, formatter: function (value, options, row) {
                if (value === 0) {
                    return '<span class="label label-info">创建成功</span>';
                } else if (value === 1) {
                    return '<span class="label label-warning">正在执行</span>';
                } else if (value === 2) {
                    return '<span class="label label-success">执行成功</span>';
                } else if (value === 3) {
                    return '<span class="label label-danger">出现异常</span>';
                }
            }
            },
            {
                label: '执行操作', name: '', width: 80, sortable: false, formatter: function (value, options, row) {
                var createReportBtn = "<a href='#' class='btn btn-primary' onclick='createReport(" + row.reportId + ")' ><i class='fa fa-plus'></i>&nbsp;生成报告</a>";
                var downloadReportBtn = "&nbsp;&nbsp;<a href='" + baseURL + "test/debugReports/downloadReport/" + row.reportId + "' class='btn btn-primary' onclick='return checkStatus(" + row.status + ")'><i class='fa fa-download'></i>&nbsp;下载</a>";
                return createReportBtn + downloadReportBtn;
            }
            }
            // 当前不做更新，页面复杂性价比不高。
            // { label: '更新时间', name: 'updateTime', width: 80 }
        ],
        viewrecords: true,
        height: $(window).height() - 150,
        rowNum: 50,
        rowList: [10, 30, 50, 100, 200],
        rownumbers: true,
        rownumWidth: 25,
        autowidth: true,
        multiselect: true,
        pager: "#jqGridPager",
        jsonReader: {
            root: "page.list",
            page: "page.currPage",
            total: "page.totalPage",
            records: "page.totalCount"
        },
        prmNames: {
            page: "page",
            rows: "limit",
            order: "order"
        },
        gridComplete: function () {
            //隐藏grid底部滚动条
            $("#jqGrid").closest(".ui-jqgrid-bdiv").css({"overflow-x": "hidden"});
        }
    });
});

var vm = new Vue({
    el: '#rrapp',
    data: {
        q: {
            caseId: null
        },
        debugCaseReport: {},
        title: null,
        showList: true
    },
    methods: {
        query: function () {
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'caseId': vm.q.caseId},
                page: 1
            }).trigger("reloadGrid");
        },
        saveOrUpdate: function () {
            if (vm.validator()) {
                return;
            }

            var url = vm.debugCaseReport.reportId == null ? "test/debugReports/save" : "test/debugReports/update";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.debugCaseReport),
                success: function (r) {
                    if (r.code === 0) {
                        // alert('操作成功', function(){
                        vm.reload();
                        // });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        // showError: function(logId) {
        // 	// 目前没有展示文件内容信息的需要。
        // 	$.get(baseURL + "test/stressFile/info/"+fileId, function(r){
        // 		// parent.layer.open({
        // 		//   title:'失败信息',
        // 		//   closeBtn:0,
        // 		//   content: r.log.error
        // 		// });
        // 	});
        // },
        update: function () {
            var reportId = getSelectedRow();
            if (reportId == null) {
                return;
            }

            $.get(baseURL + "test/debugReports/info/" + reportId, function (r) {
                vm.showList = false;
                vm.title = "修改";
                vm.debugCaseReport = r.debugCaseReport;
            });
        },
        del: function () {
            var reportIds = getSelectedRows();
            if (reportIds == null) {
                return;
            }

            confirm('确定要删除选中的记录？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/debugReports/delete",
                    contentType: "application/json",
                    data: JSON.stringify(reportIds),
                    success: function (r) {
                        if (r.code == 0) {
                            alert('操作成功', function () {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        delJtl: function () {
            var reportIds = getSelectedRows();
            if (reportIds == null) {
                return;
            }

            confirm('建议生成报告后再删除结果文件，确定删除？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/debugReports/deleteJtl",
                    contentType: "application/json",
                    data: JSON.stringify(reportIds),
                    success: function (r) {
                        if (r.code == 0) {
                            alert('操作成功', function () {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        back: function () {
            history.go(-1);
        },
        reload: function (event) {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'caseId': vm.q.caseId},
                page: page
            }).trigger("reloadGrid");
        },
        validator: function () {
            if (isBlank(vm.debugCaseReport.remark)) {
                alert("描述不能为空");
                return true;
            }
        }
    }
});

function createReport(reportIds) {
    if (!reportIds) {
        return;
    }
    confirm('结果文件超过1MB建议直接下载报告查看!', function () {
        $.ajax({
            type: "POST",
            url: baseURL + "test/debugReports/createReport",
            contentType: "application/json",
            data: JSON.stringify(numberToArray(reportIds)),
            success: function (r) {
                if (r.code == 0) {
                    vm.reload();
                    alert('后台正在异步生成!', function () {
                    });
                } else {
                    alert(r.msg);
                }
            }
        });
    });
}

function checkStatus(status) {
    if (status != 2) {
        alert('没有测试报告！');
        return false;
    }
    return true;
}