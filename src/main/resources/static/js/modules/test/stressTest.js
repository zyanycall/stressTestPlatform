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
});

var vm = new Vue({
    el: '#rrapp',
    data: {
        q: {
            caseName: null
        },
        showList: true,
        showEdit: false,
        showUpload: false,
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
            vm.showEdit= true;
            vm.showUpload = false;
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
                vm.showEdit= true;
                vm.showUpload = false;
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
            vm.showEdit= false;
            vm.showUpload = false;
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
        },
        upload: function () {
            var caseId = getSelectedRow();
            if (caseId == null) {
                return;
            }
            vm.showList = false;
            vm.showEdit= false;
            vm.showUpload = true;
            vm.title = "上传";

            var img = ['jpg','jpeg', 'png','gif', 'bmp']; //图片
            var txt = ['txt','sql','log','csv'];  //文字
            var out = ['cfg','dat','hlp','tmp'];  //文字
            var ott = ['xlsx','xls','pdf','docx','doc','pptx',];    //表格，幻灯片，WORD，PDF
            var sin = ['mpg', 'mpeg', 'avi', 'rm', 'rmvb','mov', 'wmv','asf', 'dat', 'mp4']; //视频
            var ein = ['cd','ogg','mp3','asf','wma','wav','mp3pro','rm','real','ape','module','midi','vqf']; //音频
            var spe = ['jar','war','zip','rar','tag.gz'];//压缩包
            var exe = ['exe','bat','com','msi']; //可执行文件
            var zat = ['chm','ink','jmx'];    //特殊文件
            var viw = ['ftl','htm','html','jsp']; //页面
            var rol = ['js','css'];
            initFileInput('#files', baseURL + 'test/stress/upload?token=' + token,
                img.concat(txt).concat(ott).concat(spe).concat(zat).concat(viw), {caseIds: caseId} );
        }
        // uploadFiles: function () {
        //     var caseId = getSelectedRow();
        //     if (caseId == null) {
        //         return;
        //     }
        //
        //     $('#files').fileinput('upload');
        //
        // }
    }
});

function initFileInput(formGropId, url, fileCan, extraData) {
    $(formGropId).fileinput({
        theme: "explorer", //主题
        language: 'zh', //设置语言
        uploadUrl: url,
        uploadExtraData: extraData,
        allowedFileExtensions: fileCan,//接收的文件后缀
        maxFileSize: 1024 * 20 * 100,     //1024*20Kb = 20Mb
        minFileCount: 1,
        enctype: 'multipart/form-data',
        showCaption: true,//是否显示标题
        showUpload: true, //是否显示上传按钮
        // showRemove: false, // 是否显示移除按钮
        textEncoding: 'utf-8',
        browseClass: "btn btn-primary", //按钮样式
        overwriteInitial: true,
        previewFileIcon: "<i class='glyphicon glyphicon-king'></i>",
        //previewFileIcon: '<i class="fa fa-file"></i>',
        //initialPreviewAsData: true, // defaults markup
        //preferIconicPreview: false, // 是否优先显示图标  false 即优先显示图片
        //showPreview: true,
        /*不同文件图标配置*/

        allowedPreviewTypes: false, //不预览
        previewSettings: {
            image: {width: "50px", height: "60px"},
        },
        layoutTemplates: {
            // actionUpload: '',   //取消上传按钮
            // actionZoom: ''      //取消放大镜按钮
            // actionDelete:'' //取消删除按钮
        }
    }).on('filepreupload', function (event, data, previewId, index) {//上传中
        console.info(data);
    }).on("fileuploaded", function (event, data, previewId, index) {    //一个文件上传成功
        console.log('文件上传成功！' + data);
    }).on('fileerror', function (event, data, msg) {  //一个文件上传失败
        console.log('文件上传失败！' + msg);
    }).on('filesuccessremove', function(event, id) { //上传时和这里的删除回调时，同一个文件id相同
        console.log('Uploaded thumbnail successfully removed');
    });
}

