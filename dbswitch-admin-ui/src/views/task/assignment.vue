<template>
  <div>
    <el-card>
      <div class="assignment-list-top">
        <div class="left-search-input-group">
          <div class="left-search-input">
            <el-input placeholder="请输入任务名称关键字搜索"
                      v-model="keyword"
                      :clearable=true
                      @change="searchByKeyword"
                      style="width:300px">
            </el-input>
          </div>
        </div>
        <div class="right-add-button-group">
          <el-button type="primary"
                     size="mini"
                     icon="el-icon-document-add"
                     @click="handleCreate">添加</el-button>
        </div>
      </div>

      <el-table :header-cell-style="{background:'#eef1f6',color:'#606266'}"
                :data="tableData"
                size="small"
                border>
        <el-table-column prop="id"
                         label="编号"
                         min-width="8%"></el-table-column>
        <el-table-column prop="name"
                         label="名称"
                         show-overflow-tooltip
                         min-width="30%"></el-table-column>
        <el-table-column prop="scheduleMode"
                         label="调度"
                         :formatter="stringFormatSchedule"
                         min-width="8%"></el-table-column>
        <el-table-column prop="isPublished"
                         label="已发布"
                         :formatter="boolFormatPublish"
                         :show-overflow-tooltip="true"
                         min-width="8%"></el-table-column>
        <el-table-column prop="createTime"
                         label="时间"
                         min-width="15%"></el-table-column>
        <el-table-column label="操作"
                         min-width="30%">
          <template slot-scope="scope">
            <el-button-group>
              <el-button size="small"
                         type="primary"
                         icon="el-icon-timer"
                         v-if="scope.row.isPublished===false"
                         @click="handlePublish(scope.$index, scope.row)"
                         round>发布</el-button>
              <el-button size="small"
                         type="info"
                         icon="el-icon-delete-location"
                         v-if="scope.row.isPublished===true"
                         @click="handleRetireTask(scope.$index, scope.row)"
                         round>下线</el-button>
              <el-button size="small"
                         type="danger"
                         icon="el-icon-video-play"
                         v-if="scope.row.isPublished===true"
                         @click="handleRunTask(scope.$index, scope.row)"
                         round>执行</el-button>
              <el-button size="small"
                         type="success"
                         icon="el-icon-document"
                         v-if="scope.row.isPublished===true"
                         @click="handleDetail(scope.$index, scope.row)"
                         round>详情</el-button>
              <el-button size="small"
                         type="warning"
                         icon="el-icon-edit"
                         v-if="scope.row.isPublished===false"
                         @click="handleUpdate(scope.$index, scope.row)"
                         round>修改</el-button>
              <el-button size="small"
                         type="danger"
                         icon="el-icon-delete"
                         v-if="scope.row.isPublished===false"
                         @click="handleDelete(scope.$index, scope.row)"
                         round>删除</el-button>
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

  data () {
    return {
      loading: true,
      currentPage: 1,
      pageSize: 10,
      totalCount: 2,
      keyword: null,
      tableData: [],
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
    boolFormatPublish (row, column) {
      if (row.isPublished === true) {
        return "是";
      } else {
        return "否";
      }
    },
    stringFormatSchedule (row, column) {
      if (row.scheduleMode == "MANUAL") {
        return "手动";
      } else {
        return "系统";
      }
    },
    handleCreate: function () {
      this.$router.push('/task/create')
    },
    handleDetail: function (index, row) {
      this.$router.push({ path: '/task/detail', query: { id: row.id } })
    },
    handleUpdate: function (index, row) {
      this.$router.push({ path: '/task/update', query: { id: row.id } })
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
          this.$message("任务发布成功");
          this.loadData();
        } else {
          if (res.data.message) {
            alert("任务发布失败," + res.data.message);
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
          this.$message("手动启动执行任务成功");
          this.loadData();
        } else {
          if (res.data.message) {
            alert("手动启动执行任务失败," + res.data.message);
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
          this.$message("下线任务成功");
          this.loadData();
        } else {
          if (res.data.message) {
            alert("下线任务失败," + res.data.message);
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
  created () {
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
  width: calc(100% - 100px);
  margin-right: auto;
  display: flex;
  justify-content: space-between;
}
.left-search-input {
  width: 300px;
  margin-right: auto;
  margin: 10px 5px;
}
.right-add-button-group {
  width: 100px;
  margin-left: auto;
  margin: 10px 5px;
}
</style>
