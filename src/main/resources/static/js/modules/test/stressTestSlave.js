$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'test/stressSlave/list',
        datatype: "json",
        colModel: [
            {label: '节点ID', name: 'slaveId', width: 50, key: true},
            {label: '名称', name: 'slaveName', width: 120},
            {label: 'IP地址', name: 'ip', width: 80},
            {label: '端口', name: 'port', width: 50},
            {
                label: '状态', name: 'status', width: 40, formatter: function (value, options, row) {
                if (value === 0) {
                    return '<span class="label label-danger">禁用</span>';
                } else if (value === 1) {
                    return '<span class="label label-success">启用</span>';
                }
            }
            },
            {label: '安装路径', name: 'homeDir', width: 120}
        ],
        viewrecords: true,
        height: 385,
        rowNum: 10,
        rowList: [10, 30, 50],
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
                status: 1
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

            if (isBlank(vm.stressTestSlave.port)) {
                alert("节点端口不能为空");
                return true;
            }

            if (isBlank(vm.stressTestSlave.homeDir)) {
                alert("节点Jmeter安装路径不能为空");
                return true;
            }
        }
    }
});

