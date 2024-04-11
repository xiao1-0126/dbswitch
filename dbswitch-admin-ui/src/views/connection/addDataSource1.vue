<template>
  <div>
    <el-container>
      <el-aside width="134px"></el-aside>

      <el-container>
        <el-header>
          <h3 style="font-family: 楷体" class=".h-title">请选择数据源类型</h3>
        </el-header>
        <el-main>

          <el-collapse v-model="activeNames">
            <el-collapse-item name="1">
              <template slot="title">
                <span class="sub-title">数据库</span>
              </template>
              <div>
                <el-row>
                  <el-col :span="4" v-for="(o, index) of databaseType" :key="index" :offset="1">
                    <div style="margin-top:20px;cursor: pointer">
                      <el-card :body-style="{ padding: '0px'}" shadow="hover"
                               @click.native="selectDbDriver(o)"
                               :class="selectedIndex === index ? 'active':''">
                        <div style="display: inline-block;float: left">
                          <img title="" :src="require('@/assets/icons/' + o.name +'.png')" class="image">
                        </div>
                        <div style="display: inline-block;float: left;padding: 10px 0px 0px 10px">
                          <span>{{ o.name }}</span><br>
                          <span class="tag-mdi">MDI</span>
                        </div>
                      </el-card>
                    </div>
                  </el-col>
                </el-row>

              </div>
            </el-collapse-item>
          </el-collapse>
        </el-main>
        <el-footer>
          <el-row>
            <el-button type="primary" class="next" @click="next">下一步</el-button>
            <el-button class="cancel" @click="cancel">取消</el-button>
          </el-row>
        </el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<script>
export default {
  data() {
    return {
      activeNames: ['1'],
      currentDate: new Date(),
      databaseType: [],
      selectedIndex: -1,
      selectedDataSource: {},
    };
  },

  methods: {
    loadDatabaseTypes: function () {
      this.databaseType = [];
      this.$http({
        method: "GET",
        url: "/dbswitch/admin/api/v1/connection/types"
      }).then(
          res => {
            if (0 === res.data.code) {
              this.databaseType = res.data.data;
            } else {
              alert("加载任务列表失败:" + res.data.message);
            }
          },
          function () {
            console.log("failed");
          }
      );
    },
    selectChangedDriverVersion: function (value) {
      this.connectionDriver = [];
      this.$http.get(
          "/dbswitch/admin/api/v1/connection/" + value + "/drivers"
      ).then(res => {
        if (0 === res.data.code) {
          this.connectionDriver = res.data.data;
          let varDatabaseType = this.databaseType.find(
              (item) => {
                return item.type === value;
              });
          if (varDatabaseType) {
            this.createform.sample = varDatabaseType.sample;
          }
        } else {
          this.$message.error("查询数据库可用的驱动版本失败," + res.data.message);
          this.connectionDriver = [];
        }
      });
    },
    selectDbDriver: function (value) {
      this.selectedIndex = value.id - 1;
      this.selectedDataSource = value;
    },
    next: function () {
      if (Object.keys(this.selectedDataSource).length === 0) {
        this.$message.error('请选择数据库！');
      } else {
        this.$router.push({
          path: "/connection/list/addDataSource2", query:
          this.selectedDataSource
        });
      }
    },
    cancel: function () {
      this.$router.push("/connection/list");
    },
  },
  created() {
    this.loadDatabaseTypes();
  }
}
</script>

<style scoped>
.el-header, .el-main, .el-footer {
  background-color: white;
}

.h-title {
  font-weight: bolder;
  font-size: 20px;
}

.sub-title {
  flex: 1 0 90%;
  order: 1;
  font-family: 楷体;
  font-weight: bolder;
  font-size: 18px;
}

.icon-img {
  width: 32px;
  height: 32px;
}

.instance-cards {
  position: relative;
  display: inline-block;
  margin: 0 12px 8px 0;
  padding: 8px 20px;
  box-sizing: border-box;
  background-color: #fff;
  border: 1px solid #adb0b8;
  border-radius: 2px;
  cursor: pointer;
}

.time {
  font-size: 13px;
  color: #999;
}

.bottom {
  margin-top: 13px;
  line-height: 12px;
}

.button {
  padding: 0;
  float: right;
}

.image {
  //width: 100%;
  display: inline-block;
  width: 60px;
  height: 60px;
  padding: 2px 0px 0px 2px;
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}

.clearfix:after {
  clear: both
}

.active {
  background-color: #ffffff !important;
  border: 1px solid #409EFF;
}

.tag-mdi {
  border: 0px solid;
  border-radius: 6px;
  color: #a2cdb1;
  padding: 1.5px;
  background-color: #eefff9;
}

.cancel {
  float: right;
  padding: 6px 14px;
  border: 1px solid #dcdcdd;
  cursor: pointer;
  background-color: white;
}

.next {
  float: right;
  margin-left: 20px;
  padding: 6px 14px;
  border: none;
  color: white;
  background-color: #409EFF;
  cursor: pointer;
}

</style>