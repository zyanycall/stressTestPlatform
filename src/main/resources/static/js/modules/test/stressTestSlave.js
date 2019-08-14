$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'test/stressSlave/list',
        datatype: "json",
        colModel: [
            {label: '节点ID', name: 'slaveId', width: 30, key: true},
            {label: '名称', name: 'slaveName', width: 80, sortable: false},
            {label: 'IP地址', name: 'ip', width: 50, sortable: false},
            {label: 'Jmeter端口', name: 'jmeterPort', width: 30, sortable: false},
            {label: '用户名', name: 'userName', width: 30, sortable: false},
            {label: '密码', name: 'passwd', width: 50, sortable: false, hidden: true},
            {label: 'ssh端口', name: 'sshPort', width: 30, sortable: false},
            {
                label: '状态', name: 'status', width: 30, formatter: function (value, options, row) {
                if (value === 0) {
                    return '<span class="label label-danger">禁用</span>';
                } else if (value === 1) {
                    return '<span class="label label-success">启用</span>';
                } else if (value === 2) {
                    return '<span class="label label-warning">正在执行</span>';
                } else if (value === 3) {
                    return '<span class="label label-danger">出现异常</span>';
                }
            }
            },
            {label: '安装路径', name: 'homeDir', width: 100, sortable: false},
            {label: '权重(%)', name: 'weight', width: 30, sortable: false}
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
            slaveName: null
        },
        showList: true,
        title: null,
        stressTestSlave: {}
    },
    methods: {
        query: function () {
            if (vm.q.slaveName != null) {
                vm.reload();
            }
        },
        add: function () {
            vm.showList = false;
            vm.title = "新增";
            vm.stressTestSlave = {
                status: 0,
                ip: "127.0.0.1",
                jmeterPort: 1099,
                sshPort: 22,
                homeDir: "/home/apache-jmeter-4.0",
                weight: "100"
            };
        },
        update: function () {
            var slaveId = getSelectedRow();
            if (slaveId == null) {
                return;
            }

            $.get(baseURL + "test/stressSlave/info/" + slaveId, function (r) {
                vm.showList = false;
                vm.title = "修改";
                vm.stressTestSlave = r.stressTestSlave;
            });
        },
        saveOrUpdate: function () {
            if (vm.validator()) {
                return;
            }

            var url = vm.stressTestSlave.slaveId == null ? "test/stressSlave/save" : "test/stressSlave/update";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.stressTestSlave),
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
        del: function () {
            var slaveIds = getSelectedRows();
            if (slaveIds == null) {
                return;
            }

            confirm('确定要删除选中的记录？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/stressSlave/delete",
                    contentType: "application/json",
                    data: JSON.stringify(slaveIds),
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
        batchUpdateStatus: function (value) {
            var slaveIds = getSelectedRows();
            if (slaveIds == null) {
                return;
            }

            $.ajax({
                type: "POST",
                url: baseURL + "test/stressSlave/batchUpdateStatus",
                // contentType: "application/json",
                // data: JSON.stringify(postData),
                data: {"slaveIds": slaveIds, "status": value},
                success: function (r) {
                    if (r.code == 0) {
                        alert('开始执行', function () {
                            vm.reload();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        batchUpdateStatusForce: function (value) {
            var slaveIds = getSelectedRows();
            if (slaveIds == null) {
                return;
            }

            $.ajax({
                type: "POST",
                url: baseURL + "test/stressSlave/batchUpdateStatusForce",
                // contentType: "application/json",
                // data: JSON.stringify(postData),
                data: {"slaveIds": slaveIds, "status": value},
                success: function (r) {
                    if (r.code == 0) {
                        alert('开始执行', function () {
                            vm.reload();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        batchRestart: function () {
            var slaveIds = getSelectedRows();
            if (slaveIds == null) {
                return;
            }

            $.ajax({
                type: "POST",
                url: baseURL + "test/stressSlave/batchRestart",
                // contentType: "application/json",
                // data: JSON.stringify(postData),
                data: {"slaveIds": slaveIds},
                success: function (r) {
                    if (r.code == 0) {
                        alert('开始执行', function () {
                            vm.reload();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        reload: function (event) {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'slaveName': vm.q.slaveName},
                page: page
            }).trigger("reloadGrid");
        },
        validator: function () {
            if (isBlank(vm.stressTestSlave.slaveName)) {
                alert("节点名称不能为空");
                return true;
            }

            if (isBlank(vm.stressTestSlave.ip)) {
                alert("节点IP不能为空");
                return true;
            }

            if (isBlank(vm.stressTestSlave.jmeterPort)) {
                alert("节点Jmeter端口不能为空");
                return true;
            }

            if (isBlank(vm.stressTestSlave.homeDir)) {
                alert("节点Jmeter安装路径不能为空");
                return true;
            }

            if (!isValidIP(vm.stressTestSlave.ip)) {
                alert("IP格式不合法!");
                return true;
            }

            if (!isDigits(vm.stressTestSlave.jmeterPort)) {
                alert("节点Jmeter端口号不合法!");
                return true;
            }

            if (isBlank(vm.stressTestSlave.sshPort)) {
                alert("节点ssh端口不能为空");
                return true;
            }

            if (!isDigits(vm.stressTestSlave.sshPort)) {
                alert("节点ssh端口号不合法!");
                return true;
            }

            if (!isDigits(vm.stressTestSlave.weight)) {
                alert("权重输入不合法，请输入1-99999的整数!");
                return true;
            }
        }
    }
});