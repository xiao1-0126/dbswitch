<template>
  <div>
    <el-container>
      <el-aside width="134px"></el-aside>

      <el-container>
        <el-header style="height: 80px">
          <div style="display: inline-block;float: left">
            <img title="DB" :src="require('@/assets/icons/' + this.selectedDataSource.name +'.png')" class="image">
          </div>
          <h3 style="font-family: 楷体;margin-left: 60px" class=".h-title">{{ this.selectedDataSource.name }}</h3>
        </el-header>
        <el-main>

          <el-form ref="updateform" :rules="rules" :model="updateform" label-width="120px" label-position="right"
                   size="medium" status-icon>
            <div class="f1">

              <el-form-item label="支持版本">
                  <span v-for="(o, index) of connectionDriver" :key="index" :offset="1">
                    {{ o.driverVersion }}
                    <span v-if="index !== connectionDriver.length-1">
                      、
                    </span>
                  </span>
              </el-form-item>

              <el-form-item prop="name" label="数据源名称" style="width:40%">
                <el-input v-model="updateform.name" placeholder="请输入数据源名称" auto-complete="off"></el-input>
                <label class="tips-style">数据源名称不能包含 &、<、>、"、'、(、) ，长度为1~200字符</label>
              </el-form-item>
              <el-form-item :required=true label="数据库类型">
                <label v-model="updateform.type">{{ this.selectedDataSource.name }}</label>
              </el-form-item>
              <el-form-item prop="version" label="驱动版本">
                <el-select v-model="updateform.version" placeholder="请选择驱动版本">
                  <el-option v-for="(item,index) in this.connectionDriver"
                             :key="index"
                             :label="item.driverVersion"
                             :value="item.driverVersion"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="编码格式">
                <label>utf8、utf8mb4</label>
              </el-form-item>

            </div>
            <div class="f1">
              <el-form-item label="连接模式">
                <el-radio-group v-model="updateform.mode">
                  <el-radio :label=0>默认</el-radio>
                  <el-radio :disabled="true" :label=1>专业</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item prop="address" label="连接地址">
                <el-input v-model="updateform.address" auto-complete="off" @blur="changeUrl()" style="width:20%"
                          placeholder="请输入数据源连接地址"></el-input>
                /
                <el-input v-model="updateform.port" auto-complete="off" @blur="changeUrl()" style="width:6%"
                          placeholder="Port"></el-input>
              </el-form-item>

              <el-form-item prop="databaseName" label="数据库名" style="width:24%">
                <el-input v-model="updateform.databaseName" auto-complete="off" @blur="changeUrl()"
                          placeholder="请输入数据库名"></el-input>
              </el-form-item>

              <el-form-item label="编码格式" style="width:24%">
                <el-select v-model="updateform.characterEncoding" placeholder="请选择编码格式">
                  <el-option label="utf8" value="utf8"></el-option>
                  <el-option label="utf8mb4" value="utf8mb4"></el-option>
                </el-select>
              </el-form-item>

              <el-form-item label="用户名"
                            prop="username"
                            style="width:24%">
                <el-input v-model="updateform.username"
                          auto-complete="off"></el-input>
              </el-form-item>
              <el-form-item label="密码"
                            prop="password"
                            style="width:24%">
                <el-input type="password"
                          v-model="updateform.password"
                          auto-complete="off"></el-input>
              </el-form-item>

              <el-form-item label="JDBC连接串"
                            label-width="120px"
                            prop="url"
                            style="width:85%">
                <el-tooltip placement="top">
                  <i class="el-icon-question">样例:</i>
                  <div slot="content">
                    {{ this.selectedDataSource.sample }}
                  </div>
                </el-tooltip>
                <el-input type="textarea"
                          :rows="6"
                          :spellcheck="false"
                          placeholder="请输入"
                          v-model="updateform.url"
                          auto-complete="off">
                </el-input>
                <label
                    class="tips-style">JDBC连接串（因数据库连接方式，连接参数差异较大所以需要手动拼接好），以便测试连接。</label>
              </el-form-item>


            </div>
          </el-form>
        </el-main>
        <el-footer>
          <el-row>
            <el-button class="cancel" @click="cancel">取消</el-button>
            <el-button type="primary" class="createDataSource" @click="createDataSource">创建</el-button>
            <el-button type="success" class="startTest" @click="startTest">开始检测</el-button>
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
      selectedDataSource: {},
      connectionDriver: [],
      databaseType: [],
      updateform: {
        diver: "",
        name: "",
        type: "",
        version: "",
        mode: 0,
        address: "",
        port: "",
        databaseName: "",
        characterEncoding: "",
        username: "",
        password: "",
        sample: "",
        url: "",
        oldUrl: "",
      },
      rules: {
        name: [
          {
            required: true,
            message: "数据源名称不能为空",
            trigger: "blur"
          }
        ],
        type: [
          {
            required: true,
            message: "数据库类型必须选择",
            trigger: "change"
          }
        ],
        version: [
          {
            required: true,
            message: "驱动版本必须选择",
            trigger: "change"
          }
        ],
        address: [
          {
            required: true,
            message: "连接地址不能为空",
            trigger: "change"
          }
        ],
        port: [
          {
            required: true,
            message: "连接端口号不能为空",
            trigger: "change"
          }
        ],
        databaseName: [
          {
            required: true,
            message: "数据库名不能为空",
            trigger: "change"
          }
        ],
        url: [
          {
            required: true,
            message: "Jdbc URL必须提供",
            trigger: "blur"
          }
        ],
        username: [
          {
            required: true,
            message: "用户名不能为空",
            trigger: "blur"
          }
        ],
        password: [
          {
            required: true,
            message: "密码不能为空",
            trigger: "blur"
          }
        ]
      },
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
            this.updateform.sample = varDatabaseType.sample;
          }
        } else {
          this.$message.error("查询数据库可用的驱动版本失败," + res.data.message);
          this.connectionDriver = [];
        }
      });
    },
    changeUrl: function () {
      console.log('')
    },
    startTest: function () {
      let driverClass = "";
      if (this.connectionDriver.length > 0) {
        for (let i = 0; i < this.connectionDriver.length; i++) {
          if (this.connectionDriver[i].driverVersion == this.createform.version) {
            driverClass = this.connectionDriver[i].driverClass;
            break;
          }
        }
      }
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/connection/preTest",
        data: JSON.stringify({
          name: this.createform.name,
          type: this.selectedDataSource.type,
          version: this.createform.version,
          driver: driverClass,
          url: this.createform.url,
          username: this.createform.username,
          password: this.createform.password
        })
      }).then(res => {
        if (0 === res.data.code) {
          this.$message({
            message: '测试连接成功!',
            type: 'success'
          });
        } else {
          this.$message.error('测试连接失败!');
        }
      });
    },
    createDataSource: function () {
      let driverClass = "";
      if (this.connectionDriver.length > 0) {
        for (let i = 0; i < this.connectionDriver.length; i++) {
          if (this.connectionDriver[i].driverVersion == this.createform.version) {
            driverClass = this.connectionDriver[i].driverClass;
            break;
          }
        }
      }

      this.$refs['createform'].validate(valid => {
        if (valid) {
          this.$http({
            method: "POST",
            headers: {
              'Content-Type': 'application/json'
            },
            url: "/dbswitch/admin/api/v1/connection/create",
            data: JSON.stringify({
              name: this.createform.name,
              type: this.selectedDataSource.type,
              version: this.createform.version,
              driver: driverClass,
              mode:0,
              address:this.createform.address,
              port:this.createform.port,
              databaseName:this.createform.databaseName,
              characterEncoding:this.createform.characterEncoding,
              url: this.createform.url,
              username: this.createform.username,
              password: this.createform.password
            })
          }).then(res => {
            if (0 === res.data.code) {
              this.$message({
                message: '添加连接信息成功!',
                type: 'success'
              });
              this.$router.push("/connection/list")
            } else {
              alert("添加连接信息失败:" + res.data.message);
            }
          });
        } else {
          alert("请检查输入");
        }
      });
    },
    cancel: function () {
      this.$router.push("/connection/list")
    },
  },
  created() {
    this.updateform = this.$route.query;
    this.updateform.mode = parseInt(this.updateform.mode)
    this.loadDatabaseTypes();
    if (this.databaseType.length > 0) {
      for (let i = 0; i < this.databaseType.length; i++) {
        //console.log(this.databaseType[i])
        if (this.databaseType[i].type == this.updateform.type) {
          this.selectedDataSource = this.databaseType[i].driver;
          break;
        }
      }
    }
    this.selectChangedDriverVersion(this.updateform.type);
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
  margin-left: 20px;
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
  padding: 8px 10px 0px 2px;
}

.cancel {
  float: right;
  margin-left: 20px;
  padding: 6px 14px;
  border: 1px solid #dcdcdd;
  cursor: pointer;
  background-color: white;
}

.createDataSource {
  float: right;
  margin-left: 20px;
  padding: 6px 14px;
  border: none;
  color: white;
  background-color: #409EFF;
  cursor: pointer;
}

.startTest {
  float: right;
  padding: 6px 14px;
  cursor: pointer;
}

.f1 {
  //border: 1px solid red;
  margin: 14px 0px;
  background-color: #eef0f4;
  box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04);
  padding: 4px 0px;
}

.tips-style {
  font-size: 10px;
  color: red;
}

</style>