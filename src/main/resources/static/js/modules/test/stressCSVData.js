function getUrlParam(name) { //a标签跳转获取参数
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return (r[2]);
    return null;
}

var vm = new Vue({
    el: '#csvapp',
    data: {
        CSVDataFile: [],
        loading: false,
        visible: false,
        search: null
    },
    mounted() {
        var self = this;
        $.get(baseURL + "test/stressFile/querycsvdata", function (r) {
            if (r && r.code === 0) {
                self.CSVDataFile = r.CSVDataFile;
            }
        });
    },
    methods: {
        toback: function () {
            window.location.href = 'stressCSVData.html';
        },
        tosynSingleFile: function (fileId) {
            window.location.href = 'stressSynchronizeFile.html?id=' + fileId;
        },
        deleteMasterFile: function (fileId) {
            $.ajax({
                type: "POST",
                url: baseURL + "test/stressFile/deleteMasterFile",
                contentType: "application/json",
                data: JSON.stringify(fileId),
                success: function (r) {
                    if (r.code == 0) {
                        vm.toback();
                    } else {
                        vm.loading = false;
                        alert(r.msg);
                    }
                }
            });
        },
        deleteslaveFile: function (fileId) {
            $.ajax({
                type: "POST",
                url: baseURL + "test/stressFile/deleteSlaveFile",
                contentType: "application/json",
                data: JSON.stringify(fileId),
                success: function (r) {
                    if (r.code == 0) {
                        vm.toback();
                    } else {
                        vm.loading = false;
                        alert(r.msg);
                    }
                }
            });
        },
        synchronizeFile: function (fileId) {
            if (!fileId) {
                return;
            }
            vm.loading = true;
            $.ajax({
                type: "POST",
                url: baseURL + "test/stressFile/synchronizeFile",
                contentType: "application/json",
                data: JSON.stringify(numberToArray(fileId)),
                success: function (r) {
                    if (r.code == 0) {
                        vm.toback();
                    } else {
                        vm.loading = false;
                        alert(r.msg);
                    }
                }
            });
        }, 
        open: function (name, fileId) {
            this.$prompt('请修改文件名（例：token.log）', '提示', {
                inputValue: name,
                confirmButtonText: '确定',
                cancelButtonText: '取消'
            }).then(({
                value
            }) => {
                type: 'success',
                this.updateSlaveFileName(fileId,value);
            }).catch(() => {
                this.$message({
                    type: 'info',
                    message: '取消输入'
                });
            });
        }, 
        updateSlaveFileName: function (fileId,realname) {
            $.ajax({
                type: "POST",
                url: baseURL + "test/stressFile/updateSlaveFileName",
                dataType: "json",
                contentType: "application/json",
                data: JSON.stringify({
                    fileId,
                    realname
                }),
                success: function (r) {
                    if (r.code == 0) {
                        vm.toback();
                    } else {
                        vm.loading = false;
                        alert(r.msg);
                    }
                }
            });
        },
        downloadFile: function (fileId) {
            top.location.href = baseURL + "test/stressFile/downloadFile/" + fileId;
        }
    }
});

