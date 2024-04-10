<template>
  <div>
    <el-container>
      <el-aside width="134px"></el-aside>

      <el-container>
        <el-header style="height: 80px">
          <div style="display: inline-block;float: left">
            <img title="DB" :src="require('@/assets/icons/' + this.dataSourceInfo.typeName +'.png')" class="image">
          </div>
          <h3 style="font-family: 楷体;margin-left: 60px" class=".h-title">{{ this.dataSourceInfo.typeName }}</h3>
        </el-header>
        <el-main>

          <el-form  :model="dataSourceInfo" label-width="120px" label-position="right"
                   size="medium" status-icon>
            <div class="f1">

              <el-form-item label="支持版本">
                  <span>
                    {{ this.dataSourceInfo.version }}
                  </span>
              </el-form-item>

              <el-form-item prop="name" label="数据源名称" style="width:40%">
                <el-input :readonly=true v-model="dataSourceInfo.name" placeholder="请输入数据源名称" auto-complete="off"></el-input>
                <label class="tips-style">数据源名称不能包含 &、<、>、"、'、(、) ，长度为1~200字符</label>
              </el-form-item>
              <el-form-item :required=true label="数据库类型">
                <label v-model="dataSourceInfo.typeName">{{ this.dataSourceInfo.typeName }}</label>
              </el-form-item>
              <el-form-item prop="version" label="驱动版本" style="width:40%">
                <el-input  v-model="dataSourceInfo.version" placeholder="请选择驱动版本" auto-complete="off" :readonly=true></el-input>
              </el-form-item>

              <el-form-item label="编码格式">
                <label>{{dataSourceInfo.characterEncoding}}</label>
              </el-form-item>

            </div>
            <div class="f1">
              <el-form-item label="连接模式">
                <el-radio-group v-model="dataSourceInfo.mode">
                  <el-radio :label=0>默认</el-radio>
                  <el-radio :disabled="true" :label=1>专业</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item prop="address" label="连接地址">
                <el-input :readonly=true v-model="dataSourceInfo.address" auto-complete="off" style="width:20%"
                          placeholder="请输入数据源连接地址"></el-input>
                /
                <el-input :readonly=true v-model="dataSourceInfo.port" auto-complete="off"  style="width:6%"
                          placeholder="Port"></el-input>
              </el-form-item>

              <el-form-item prop="databaseName" label="数据库名" style="width:24%">
                <el-input :readonly=true v-model="dataSourceInfo.databaseName" auto-complete="off"
                          placeholder="请输入数据库名"></el-input>
              </el-form-item>

              <el-form-item label="编码格式" style="width:24%">
                <el-input :readonly=true v-model="dataSourceInfo.characterEncoding" auto-complete="off"
                          placeholder="请选择编码格式"></el-input>
              </el-form-item>

              <el-form-item label="用户名"
                            prop="username"
                            style="width:24%">
                <el-input :readonly=true v-model="dataSourceInfo.username"
                          auto-complete="off"></el-input>
              </el-form-item>
              <el-form-item label="密码"
                            prop="password"
                            style="width:24%">
                <el-input :readonly=true type="password"
                          v-model="dataSourceInfo.password"
                          auto-complete="off"></el-input>
              </el-form-item>

              <el-form-item label="JDBC连接串"
                            label-width="120px"
                            prop="url"
                            style="width:85%">
                <el-input :readonly=true type="textarea"
                          :rows="6"
                          :spellcheck="false"
                          placeholder="请输入"
                          v-model="dataSourceInfo.url"
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
      dataSourceInfo: {
        name: "",
        type: "",
        typeName: "",
        version: "",
        driver: "",
        mode:0,
        address:"",
        port:"",
        databaseName:"",
        characterEncoding:"",
        url: "",
        username: "",
        password: ""
      },
    };
  },

  methods: {
    cancel: function () {
      this.$router.push("/connection/list");
    },
  },
  created() {
    this.dataSourceInfo = this.$route.query;
    this.dataSourceInfo.mode = parseInt(this.dataSourceInfo.mode)
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