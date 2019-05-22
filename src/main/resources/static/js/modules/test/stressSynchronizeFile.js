function getUrlParam(name) { //a标签跳转获取参数
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return (r[2]);
    return null;
}

var vmsyn = new Vue({
    el: '#fileapp',
    data: {
        list: [],
        loading: false
    },
    mounted() {
        var self = this;
        $.get(baseURL + "test/stressSlave/list?limit=50&page=1", function (r) {
            if (r && r.code === 0 && r.page.list && r.page.list.length) {
                self.list = r.page.list;
            }
        });
    },
    methods: {
        toback: function () {
            window.location.href = 'stressCSVData.html';
        },
        synchronizeSingleSalve: function (slaveId,ip) {
            var fileId = getUrlParam('id');
            if (fileId == null) {
                return;
            }
            if (ip == '127.0.0.1'){
                alert('本机不能同步');
                return;
            }
            // 按键放多次点击
            vmsyn.loading = true;
            var url = "test/stressFile/synchronizeSingleSalve";
            var _this = this;
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify({
                    "fileId": fileId, "slaveId": slaveId
                }),
                success: function (r) {
                    if (r.code === 0) {
                        alert('服务器:'+ip+' 同步成功');
                        vmsyn.loading = false;
                    } else {
                        alert(r.msg);
                        vmsyn.loading = false;
                    }
                }
            });
        }
    }
});

