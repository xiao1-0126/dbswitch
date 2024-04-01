<template>
  <div>
    <el-container>
      <el-aside width="134px"></el-aside>
      <el-container>
        <el-header>
          <h3 style="font-family: 楷体;" class=".h-title">请选择数据源类型</h3>
        </el-header>
        <el-main>

          <el-collapse v-model="activeNames" @change="handleChange">
            <el-collapse-item name="1">
              <template slot="title">
                <span class="sub-title">关系型数据库</span>
              </template>
<!--              <div class="instance-cards">-->
<!--                <img class="icon-img" :src="db2Icon" alt="DB2 Icon"/>-->
<!--                <p style="white-space: nowrap; text-overflow: ellipsis; overflow: hidden;">DB2</p>-->
<!--              </div>-->
              <div>

                <el-row>
                  <el-col :span="3" v-for="(o, index) in 5" :key="o" :offset="index > 0 ? 2 : 0">
                    <el-card :body-style="{ padding: '0px' }">
                      <img :src="db2Icon" class="image">
                      <div style="padding: 8px">
                        <span>DB2</span>
                        <div class="bottom clearfix">
<!--                          <time class="time">{{ currentDate }}</time>-->
                          <el-button type="text" class="button">选择</el-button>
                        </div>
                      </div>
                    </el-card>
                  </el-col>
                </el-row>

              </div>
            </el-collapse-item>
            <el-collapse-item name="2">
              <template class="sub-title" slot="title">
                <span class="sub-title">大数据存储</span>
              </template>
              <div>控制反馈：通过界面样式和交互动效让用户可以清晰的感知自己的操作；</div>
              <div>页面反馈：操作后，通过页面元素的变化清晰地展现当前状态。</div>
            </el-collapse-item>
            <el-collapse-item name="3">
              <template class="sub-title" slot="title">
                <span class="sub-title">半结构化存储</span>
              </template>
              <div>简化流程：设计简洁直观的操作流程；</div>
              <div>清晰明确：语言表达清晰且表意明确，让用户快速理解进而作出决策；</div>
              <div>帮助用户识别：界面简单直白，让用户快速识别而非回忆，减少用户记忆负担。</div>
            </el-collapse-item>
          </el-collapse>


        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
export default {
  data() {
    return {
      activeNames: ['1'],
      db2Icon: require('@/assets/icons/db2.png'),
      currentDate: new Date()
    };
  },

  methods: {
    handleChange(val) {
      console.log(val);
    }
  }
}
</script>

<style scoped>
.el-header, .el-main {
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
  display: block;
  width: 64px;
  height: 64px;
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}

.clearfix:after {
  clear: both
}
</style>