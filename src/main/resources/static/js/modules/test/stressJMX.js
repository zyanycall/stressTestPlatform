function getUrlParam(name) { //a标签跳转获取参数
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return (r[2]);
    return null;
}

var vm0_0 = new Vue({
    el: '#jmxapp',
    data: {
        jmxFile: []
    },
    mounted() {
        var fileId = getUrlParam('id');
        if (fileId == null) {
            return;
        }
        var self = this;
        $.get(baseURL + "test/stressFile/queryJMXFile/" + fileId, function (r) {
            if (r && r.code === 0 && r.jmxFile && r.jmxFile.length) {
                self.jmxFile = r.jmxFile;
            }
        });
    },
    methods: {
        cancelchange: function () {
            window.location.href = 'stressTestFile.html';
        },
        savejmx: function () {
            var fileId = getUrlParam('id');
            console.log("fileId" + fileId);
            if (fileId == null) {
                return;
            }

            var url = "test/stressFile/updateJMXFile";
            var _this = this;
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify({
                    "fileId": fileId, "jmxThreadGroup": _this.jmxFile
                }),
                success: function (r) {
                    if (r.code === 0) {
                        window.location.href = 'stressTestFile.html';
                    } else {
                        alert(r.msg);
                    }
                }
            });

        }
    }
});

