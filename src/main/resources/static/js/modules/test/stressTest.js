$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'test/stress/list',
        datatype: "json",
        colModel: [
            {label: '用例ID', name: 'caseId', width: 50, key: true},
            {label: '名称', name: 'caseName', sortable: false, width: 150},
            {label: '添加时间', name: 'addTime', width: 90},
            {label: '项目', name: 'project', sortable: false, width: 80},
            {label: '模块', name: 'module', sortable: false, width: 80},
            {label: '操作人', name: 'operator', sortable: false, width: 60},
            // { label: 'cron表达式 ', name: 'cronExpression', width: 100 },
            { label: '备注', name: 'remark', sortable: false, width: 110 }
        ],
        viewrecords: true,
        height: 385,
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


    new AjaxUpload('#upload', {
        action: baseURL + 'test/stress/upload?token=' + token,
        name: 'file',
        autoSubmit: true,
        responseType: "json",
        onSubmit: function (file, extension) {
            var caseId = getSelectedRow();
            if (caseId == null) {
                return false;
            }
            // if (!(extension && /^(txt|jmx)$/.test(extension.toLowerCase()))) {
            //     alert('只支持jmx、txt格式的用例相关文件！');
            //     return false;
            // }

            this.setData({caseIds: caseId})
        },
        // onChange: function(file, ext){
            // debugger
            // if(!(ext && (/^(jmx)$/.test(ext))){
            //     alert("只支持jmx格式的文件！");
            //     return false
            // }
        // },
        onComplete: function (file, r) {
            if (r.code == 0) {
                // alert(r.url);
                alert('操作成功', function () {
                    vm.reload();
                });
            } else {
                alert(r.msg);
            }
        }
    });

});

var vm = new Vue({
    el: '#rrapp',
    data: {
        q: {
            caseName: null
        },
        showList: true,
        title: null,
        stressCase: {}
    },
    methods: {
        query: function () {
            if (vm.q.caseName != null) {
                vm.reload();
            }
        },
        add: function () {
            vm.showList = false;
            vm.title = "新增";
            vm.stressCase = {};
        },
        update: function () {
            var caseId = getSelectedRow();
            if (caseId == null) {
                return;
            }

            $.get(baseURL + "test/stress/info/" + caseId, function (r) {
                vm.showList = false;
                vm.title = "修改";
                vm.stressCase = r.stressCase;
            });
        },
        saveOrUpdate: function () {
            if (vm.validator()) {
                return;
            }

            var url = vm.stressCase.caseId == null ? "test/stress/save" : "test/stress/update";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.stressCase),
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
            var caseIds = getSelectedRows();
            if (caseIds == null) {
                return;
            }

            confirm('确定要删除选中的记录？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/stress/delete",
                    contentType: "application/json",
                    data: JSON.stringify(caseIds),
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
                postData: {'caseName': vm.q.caseName},
                page: page
            }).trigger("reloadGrid");
        },
        validator: function () {
            if (isBlank(vm.stressCase.caseName)) {
                alert("用例名称不能为空");
                return true;
            }

            if (isBlank(vm.stressCase.project)) {
                alert("项目名称不能为空");
                return true;
            }

            if (isBlank(vm.stressCase.module)) {
                alert("模块名称不能为空");
                return true;
            }

            if (isBlank(vm.stressCase.operator)) {
                alert("操作人不能为空");
                return true;
            }
        }
        // upload: function () {
        //     debugger
        //
        //     var caseId = getSelectedRow();
        //     if (caseId == null) {
        //         return;
        //     }
        //     ajaxUpload.submit();
        // }
    }
});

