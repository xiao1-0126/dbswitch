<template>
  <div>
    <el-card>
      <el-row>
        <el-button size="mini" icon="el-icon-switch-button" :disabled=isSelected plain @click="batchStart()">启动
        </el-button>
        <el-button size="mini" icon="el-icon-video-pause" :disabled=isSelected plain @click="batchStop()">停止</el-button>
        <span style="color:#e9e9f3;">&nbsp;&nbsp;|&nbsp;&nbsp;</span>
        <el-button size="mini" plain>导入任务</el-button>
        <el-button size="mini" :disabled=isSelected plain @click="batchExport()">导出任务</el-button>
        <!--        <div class="right-add-button-group">-->
        <el-button class="right-add-button-group" type="primary"
                   size="mini"
                   icon="el-icon-document-add"
                   @click="handleCreate">创建任务
        </el-button>
        <!--        </div>-->
      </el-row>
      <div class="assignment-list-top">
        <div class="left-search-input-group">
          <div class="left-search-input">
            <el-input size="mini" placeholder="请输入任务名称关键字搜索"
                      v-model="keyword"
                      :clearable=true
                      @change="searchByKeyword"
                      style="width:100%">
              <el-button @click="searchByKeyword" slot="append" icon="el-icon-search"></el-button>
            </el-input>
          </div>
        </div>

      </div>

      <el-table :header-cell-style="{background:'#eef1f6',color:'#606266'}"
                :data="tableData"
                size="small"
                border
                @selection-change="handleSelectionChange">
        <el-table-column prop="id"
                         label="编号"
                         type="selection"
                         min-width="6%"></el-table-column>
        <el-table-column prop="name"
                         label="任务名称"
                         show-overflow-tooltip
                         min-width="10%"></el-table-column>
        <el-table-column prop="scheduleMode"
                         label="集成模式"
                         :formatter="stringFormatSchedule"
                         min-width="8%"></el-table-column>
        <el-table-column
            label="源端数据源"
            min-width="10%">
          <template slot-scope="scope">
            <el-popover trigger="hover" placement="top">
              <p>源端数据源：{{ scope.row.sourceSchema }}</p>
              <p>源端数据源类型：{{ scope.row.sourceType }}</p>
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium">{{ scope.row.sourceSchema }}</el-tag>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column
            label="目标端数据源"
            min-width="10%">
          <template slot-scope="scope">
            <el-popover trigger="hover" placement="top">
              <p>目标端数据源：{{ scope.row.targetSchema }}</p>
              <p>目标端数据源类型：{{ scope.row.targetType }}</p>
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium">{{ scope.row.targetSchema }}</el-tag>
              </div>
            </el-popover>
          </template>
        </el-table-column>

        <el-table-column
            label="运行状态"
            show-overflow-tooltip
            min-width="10%">
          <template slot-scope="scope">
            <el-icon class="el-icon-success color-success" v-if="scope.row.runStatus == '执行成功'"></el-icon>
            <el-icon class="el-icon-error color-error" v-if="scope.row.runStatus == '执行异常'"></el-icon>
            <el-icon class="el-icon-remove color-cancel" v-if="scope.row.runStatus == '任务取消'"></el-icon>
            <el-icon class="el-icon-video-play color-running" v-if="scope.row.runStatus == '执行中'"></el-icon>
            <el-icon class="el-icon-loading color-await" v-if="scope.row.runStatus == '待执行'"></el-icon>
            <span>{{ scope.row.runStatus }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="isPublished"
                         label="任务状态"
                         :formatter="boolFormatPublish"
                         :show-overflow-tooltip="true"
                         min-width="8%"></el-table-column>
        <el-table-column prop="createTime"
                         label="开始调度时间"
                         min-width="12%"></el-table-column>
        <el-table-column label="操作"
                         min-width="40%">
          <template slot-scope="scope">
            <el-button-group>
              <el-button size="small"
                         type="primary"
                         icon="el-icon-timer"
                         @click="handlePublish(scope.$index, scope.row)"
                         round>调度日志
              </el-button>
              <el-button size="small"
                         type="primary"
                         icon="el-icon-timer"
                         v-if="scope.row.isPublished===false"
                         @click="handlePublish(scope.$index, scope.row)"
                         round>启动
              </el-button>
              <el-button size="small"
                         type="info"
                         icon="el-icon-delete-location"
                         v-if="scope.row.isPublished===true"
                         @click="handleRetireTask(scope.$index, scope.row)"
                         round>停止
              </el-button>
              <el-button size="small"
                         type="danger"
                         icon="el-icon-video-play"
                         @click="handleRunTask(scope.$index, scope.row)"
                         round>手工调度
              </el-button>
              <el-button size="small"
                         type="success"
                         icon="el-icon-document"
                         @click="handleDetail(scope.$index, scope.row)"
                         round>详情
              </el-button>
              <el-button size="small"
                         type="warning"
                         icon="el-icon-edit"
                         @click="handleUpdate(scope.$index, scope.row)"
                         round>修改
              </el-button>
              <el-button size="small"
                         type="danger"
                         icon="el-icon-delete"
                         v-if="scope.row.isPublished===false"
                         @click="handleDelete(scope.$index, scope.row)"
                         round>删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
      <div class="page"
           align="right">
        <el-pagination @size-change="handleSizeChange"
                       @current-change="handleCurrentChange"
                       :current-page="currentPage"
                       :page-sizes="[5, 10, 20, 40]"
                       :page-size="pageSize"
                       layout="total, sizes, prev, pager, next, jumper"
                       :total="totalCount"></el-pagination>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {

  data() {
    return {
      loading: true,
      currentPage: 1,
      pageSize: 10,
      totalCount: 2,
      keyword: null,
      tableData: [],
      isSelected: true,
      idsSelected:[]
    };
  },
  methods: {
    loadData: function () {
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/assignment/list",
        data: JSON.stringify({
          searchText: this.keyword,
          page: this.currentPage,
          size: this.pageSize
        })
      }).then(res => {
            if (0 === res.data.code) {
              this.currentPage = res.data.pagination.page;
              this.pageSize = res.data.pagination.size;
              this.totalCount = res.data.pagination.total;
              this.tableData = res.data.data;
            } else {
              alert("加载任务列表失败:" + res.data.message);
            }
          },
          function () {
            console.log("load assignments list failed");
          }
      );
    },
    searchByKeyword: function () {
      this.currentPage = 1;
      this.loadData();
    },
    boolFormatPublish(row, column) {
      if (row.isPublished === true) {
        return "启动";
      } else {
        return "停止";
      }
    },
    stringFormatSchedule(row, column) {
      if (row.scheduleMode == "MANUAL") {
        return "手动";
      } else {
        return "定时";
      }
    },
    handleSelectionChange(val) {
      if (val.length > 0) {
        this.isSelected = false;
        this.idsSelected.push(val.map(item => item.id));
      }else {
        this.isSelected = true;
        this.idsSelected = []
      }
    },
    batchStart(){
      console.log(this.idsSelected)
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/assignment/deploy?ids=" + this.idsSelected,
      }).then(res => {
        if (0 === res.data.code) {
          this.$message({
            message: '任务发布成功!',
            type: 'success'
          });
          this.loadData();
        } else {
          if (res.data.message) {
            this.$message.error("任务发布失败," + res.data.message);
          }
        }
      });
    },
    batchStop(){
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/assignment/retire?ids=" + this.idsSelected,
      }).then(res => {
        if (0 === res.data.code) {
          this.$message({
            message: '下线任务成功!',
            type: 'success'
          });
          this.loadData();
        } else {
          if (res.data.message) {
            this.$message.error("下线任务失败," + res.data.message);
          }
        }
      });
    },
    batchExport(){
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/assignment/export?ids=" + this.idsSelected,
        responseType: 'blob',
      }).then(res => {
        debugger
        if (200 === res.status) {
          this.downloadFile(res)
          this.$message({
            message: '导出任务成功!',
            type: 'success'
          });
          this.loadData();
        } else {
          if (res.data.message) {
            this.$message.error("导出任务失败," + res.data.message);
          }
        }
      });
    },
    downloadFile: function (resp) {
      const headers = resp.headers;
      const contentType = headers['content-type'];
      if (!resp.data) {
        console.error('响应异常：', resp);
        return false;
      } else {
        console.info('下载文件：', resp);
        const blob = new Blob([resp.data], {
          type: contentType
        });

        const contentDisposition = resp.headers['Content-disposition'];
        let fileName = 'unknown';
        if (contentDisposition) {
          debugger
          fileName = window.decodeURI(resp.headers['Content-disposition'].split('=')[1]);
        }
        console.log('文件名称：', fileName);
        this.downFile(blob, fileName);
      }
    },
    /* 下载方法 : 根据blob下载 */
    downFile: function (blob, fileName) {
      // 非IE下载
      if ('download' in document.createElement('a')) {
        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob); // 创建下载的链接
        link.download = fileName; // 下载后文件名
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click(); // 点击下载
        window.URL.revokeObjectURL(link.href); // 释放掉blob对象
        document.body.removeChild(link); // 下载完成移除元素
      } else {
        // IE10+下载
        window.navigator.msSaveBlob(blob, fileName);
      }
    },
    handleCreate: function () {
      this.$router.push('/task/create')
    },
    handleDetail: function (index, row) {
      this.$router.push({path: '/task/detail', query: {id: row.id}})
    },
    handleUpdate: function (index, row) {
      this.$router.push({path: '/task/update', query: {id: row.id}})
    },
    handleDelete: function (index, row) {
      this.$confirm(
          "此操作将此任务ID=" + row.id + "删除么, 是否继续?",
          "提示",
          {
            confirmButtonText: "确定",
            cancelButtonText: "取消",
            type: "warning"
          }
      ).then(() => {
        this.$http.delete(
            "/dbswitch/admin/api/v1/assignment/delete/" + row.id
        ).then(res => {
          if (0 === res.data.code) {
            this.loadData();
          } else {
            if (res.data.message) {
              alert("删除任务失败:" + res.data.message);
            }
          }
        });
      });
    },
    handlePublish: function (index, row) {
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/assignment/deploy?ids=" + row.id,
      }).then(res => {
        if (0 === res.data.code) {
          this.$message({
            message: '任务发布成功!',
            type: 'success'
          });
          this.loadData();
        } else {
          if (res.data.message) {
            this.$message.error("任务发布失败," + res.data.message);
          }
        }
      });
    },
    handleRunTask: function (index, row) {
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/assignment/run",
        data: JSON.stringify([row.id])
      }).then(res => {
        if (0 === res.data.code) {
          this.$message({
            message: '手动启动执行任务成功!',
            type: 'success'
          });
          this.loadData();
        } else {
          if (res.data.message) {
            this.$message.error("手动启动执行任务失败," + res.data.message);
          }
        }
      });
    },
    handleRetireTask: function (index, row) {
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/assignment/retire?ids=" + row.id,
      }).then(res => {
        if (0 === res.data.code) {
          this.$message({
            message: '下线任务成功!',
            type: 'success'
          });
          this.loadData();
        } else {
          if (res.data.message) {
            this.$message.error("下线任务失败," + res.data.message);
          }
        }
      });
    },
    handleSizeChange: function (pageSize) {
      this.loading = true;
      this.pageSize = pageSize;
      this.loadData();
    },

    handleCurrentChange: function (currentPage) {
      this.loading = true;
      this.currentPage = currentPage;
      this.loadData();
    }
  },
  created() {
    this.loadData();
  },
};
</script>

<style scoped>
.el-card,
.el-message {
  width: 100%;
  height: 100%;
  overflow: auto;
}

.el-table {
  width: 100%;
  height: 100%;
}

/deep/ .el-table--border .el-table__cell {
  border-right: 0px solid red !important;
}

.demo-table-expand {
  font-size: 0;
}

.demo-table-expand label {
  width: 90px;
  color: #99a9bf;
}

.demo-table-expand .el-form-item {
  margin-right: 0;
  margin-bottom: 0;
  width: 50%;
}

.el-input.is-disabled .el-input__inner {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
  color: #c0c4cc;
  cursor: pointer;
}

.assignment-list-top {
  width: 100%;
  display: flex;
  justify-content: space-between;
}

.left-search-input-group {
  width: 100%;
  margin-right: auto;
  display: flex;
  justify-content: space-between;
}

.left-search-input {
  width: 100%;
  margin-right: auto;
  margin: 10px 0px;
}

.right-add-button-group {
  width: 100px;
  margin-left: auto;
/ / margin: 0 px 0 px;
  float: right;
}

.color-await {
  color: #a0a6b8 !important;
}

.color-running {
  color: #6cdbbc !important;
}

.color-error {
  color: #ff9c86 !important;
}

.color-cancel {
  color: #a0a6b8 !important;
}

.color-success {
  color: #6cdbbc !important;
}

.btn-common {

}

</style>
