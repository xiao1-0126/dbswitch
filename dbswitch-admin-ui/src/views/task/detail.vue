<template>
  <el-card>
    <el-form :model="updateform"
             status-icon
             ref="updateform">
      <el-descriptions size="small"
                       :column="1"
                       label-class-name="el-descriptions-item-label-class"
                       border>
        <el-descriptions-item label="任务名称">{{updateform.name}}</el-descriptions-item>
        <el-descriptions-item label="任务描述">{{updateform.description}}</el-descriptions-item>
        <el-descriptions-item label="调度方式">
          <span v-if="updateform.scheduleMode == 'MANUAL'">
            手动执行
          </span>
          <span v-if="updateform.scheduleMode == 'SYSTEM_SCHEDULED'">
            系统调度
          </span>
        </el-descriptions-item>
        <el-descriptions-item v-if="updateform.scheduleMode == 'SYSTEM_SCHEDULED'"
                              label="CRON表达式">{{updateform.cronExpression}}</el-descriptions-item>
        <el-descriptions-item label="源端数据源">[{{updateform.sourceConnectionId}}]{{updateform.sourceConnectionName}}</el-descriptions-item>
        <el-descriptions-item label="源端schema">{{updateform.sourceSchema}}</el-descriptions-item>
        <el-descriptions-item label="源端表类型">{{updateform.tableType}}</el-descriptions-item>
        <el-descriptions-item label="源端表选择方式">
          <span v-if="updateform.includeOrExclude == 'INCLUDE'">
            包含表
          </span>
          <span v-if="updateform.includeOrExclude == 'EXCLUDE'">
            排除表
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="源端表名列表">
          <span v-show="updateform.includeOrExclude == 'INCLUDE' && (!updateform.sourceTables || updateform.sourceTables.length==0)"><b>所有物理表</b></span>
          <p v-for="item in updateform.sourceTables"
             v-bind:key="item">{{item}}</p>
        </el-descriptions-item>
        <el-descriptions-item label="目地端数据源">[{{updateform.targetConnectionId}}]{{updateform.targetConnectionName}}</el-descriptions-item>
        <el-descriptions-item label="目地端schema">{{updateform.targetSchema}}</el-descriptions-item>
        <el-descriptions-item label="自动同步模式">
          <span v-if="updateform.autoSyncMode == 2">
            目标端建表并同步数据
          </span>
          <span v-if="updateform.autoSyncMode == 1">
            目标端只创建物理表
          </span>
          <span v-if="updateform.autoSyncMode == 0">
            目标端只同步表里数据
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="建表字段自增"
                              v-if=" updateform.autoSyncMode!==0 ">{{updateform.targetAutoIncrement}}</el-descriptions-item>
        <el-descriptions-item label="数据批次大小"
                              v-if=" updateform.autoSyncMode!==1 ">{{updateform.batchSize}}</el-descriptions-item>
        <el-descriptions-item label="表名大小写转换"
                              v-if=" updateform.autoSyncMode!==0 ">
          <span v-if="updateform.tableNameCase == 'NONE'">
            无转换
          </span>
          <span v-if="updateform.tableNameCase == 'UPPER'">
            转大写
          </span>
          <span v-if="updateform.tableNameCase == 'LOWER'">
            转小写
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="列名大小写转换"
                              v-if=" updateform.autoSyncMode!==0 ">
          <span v-if="updateform.columnNameCase == 'NONE'">
            无转换
          </span>
          <span v-if="updateform.columnNameCase == 'UPPER'">
            转大写
          </span>
          <span v-if="updateform.columnNameCase == 'LOWER'">
            转小写
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="表名映射规则">
          <span v-show="!updateform.tableNameMapper || updateform.tableNameMapper.length==0">[映射关系为空]</span>
          <table v-if="updateform.tableNameMapper && updateform.tableNameMapper.length>0"
                 class="name-mapper-table">
            <tr>
              <th>表名匹配的正则名</th>
              <th>替换的目标值</th>
            </tr>
            <tr v-for='(item,index) in updateform.tableNameMapper'
                :key="index">
              <td>{{item['fromPattern']}}</td>
              <td>{{item['toValue']}}</td>
            </tr>
          </table>
        </el-descriptions-item>
        <el-descriptions-item label="字段名映射规则">
          <span v-show="!updateform.columnNameMapper || updateform.columnNameMapper.length==0">[映射关系为空]</span>
          <table v-if="updateform.columnNameMapper && updateform.columnNameMapper.length>0"
                 class="name-mapper-table">
            <tr>
              <th>字段名匹配的正则名</th>
              <th>替换的目标值</th>
            </tr>
            <tr v-for='(item,index) in updateform.columnNameMapper'
                :key="index">
              <td>{{item['fromPattern']}}</td>
              <td>{{item['toValue']}}</td>
            </tr>
          </table>
        </el-descriptions-item>
      </el-descriptions>

    </el-form>
    <el-button type="primary"
               size="mini"
               icon="el-icon-arrow-left"
               @click="handleGoBack">
      返回
    </el-button>
  </el-card>
</template>

<script>
export default {

  data () {
    return {
      updateform: {
        id: 0,
        name: "",
        description: "",
        scheduleMode: "MANUAL",
        cronExpression: "",
        sourceConnectionId: '请选择',
        sourceSchema: "",
        tableType: "TABLE",
        includeOrExclude: "",
        sourceTables: [],
        tableNameMapper: [],
        columnNameMapper: [],
        tableNameCase: 'NONE',
        columnNameCase: 'NONE',
        targetConnectionId: '请选择',
        targetDropTable: true,
        targetOnlyCreate: false,
        autoSyncMode: 2,
        targetSchema: "",
        batchSize: 5000
      },
      sourceConnection: {},
      targetConnection: {},
      sourceConnectionSchemas: [],
      sourceSchemaTables: [],
      targetConnectionSchemas: [],
    }
  },
  methods: {
    loadAssignmentDetail: function () {
      this.$http.get(
        "/dbswitch/admin/api/v1/assignment/detail/id/" + this.$route.query.id
      ).then(res => {
        if (0 === res.data.code) {
          let detail = res.data.data;
          let varAutoSyncMode = 2;
          if (detail.configuration.targetDropTable && detail.configuration.targetOnlyCreate) {
            varAutoSyncMode = 1;
          } else if (!detail.configuration.targetDropTable && !detail.configuration.targetOnlyCreate) {
            varAutoSyncMode = 0;
          } else {
            varAutoSyncMode = 2;
          }
          this.updateform = {
            id: detail.id,
            name: detail.name,
            description: detail.description,
            scheduleMode: detail.scheduleMode,
            cronExpression: detail.cronExpression,
            sourceConnectionId: detail.configuration.sourceConnectionId,
            sourceConnectionName: detail.configuration.sourceConnectionName,
            sourceSchema: detail.configuration.sourceSchema,
            tableType: detail.configuration.tableType,
            includeOrExclude: detail.configuration.includeOrExclude,
            sourceTables: detail.configuration.sourceTables,
            tableNameMapper: detail.configuration.tableNameMapper,
            columnNameMapper: detail.configuration.columnNameMapper,
            tableNameCase: detail.configuration.tableNameCase,
            columnNameCase: detail.configuration.columnNameCase,
            targetConnectionId: detail.configuration.targetConnectionId,
            targetConnectionName: detail.configuration.targetConnectionName,
            targetDropTable: detail.configuration.targetDropTable,
            targetOnlyCreate: detail.configuration.targetOnlyCreate,
            targetAutoIncrement: detail.configuration.targetAutoIncrement,
            autoSyncMode: varAutoSyncMode,
            targetSchema: detail.configuration.targetSchema,
            batchSize: detail.configuration.batchSize
          }
          this.selectChangedSourceConnection(this.updateform.sourceConnectionId)
          this.selectUpdateChangedSourceSchema(this.updateform.sourceSchema)
          this.selectChangedTargetConnection(this.updateform.targetConnectionId)
        } else {
          if (res.data.message) {
            alert("查询任务失败," + res.data.message);
          }
        }
      });
    },
    selectChangedSourceConnection: function (value) {
      this.sourceConnection = this.connectionNameList.find(
        (item) => {
          return item.id === value;
        });

      this.sourceConnectionSchemas = [];
      this.$http.get(
        "/dbswitch/admin/api/v1/connection/schemas/get/" + value
      ).then(res => {
        if (0 === res.data.code) {
          this.sourceConnectionSchemas = res.data.data;
        } else {
          this.$message.error("查询来源端数据库的Schema失败," + res.data.message);
          this.sourceConnectionSchemas = [];
        }
      });
    },
    selectUpdateChangedSourceSchema: function (value) {
      this.sourceSchemaTables = [];
      if ('TABLE' === this.updateform.tableType) {
        this.$http.get(
          "/dbswitch/admin/api/v1/connection/tables/get/" + this.updateform.sourceConnectionId + "?schema=" + value
        ).then(res => {
          if (0 === res.data.code) {
            this.sourceSchemaTables = res.data.data;
          } else {
            this.$message.error("查询来源端数据库在指定Schema下的物理表列表失败," + res.data.message);
            this.sourceSchemaTables = [];
          }
        });
      } else {
        this.$http.get(
          "/dbswitch/admin/api/v1/connection/views/get/" + this.updateform.sourceConnectionId + "?schema=" + value
        ).then(res => {
          if (0 === res.data.code) {
            this.sourceSchemaTables = res.data.data;
          } else {
            this.$message.error("查询来源端数据库在指定Schema下的视图表列表失败," + res.data.message);
            this.sourceSchemaTables = [];
          }
        });
      }
    },
    selectChangedTargetConnection: function (value) {
      this.targetConnection = this.connectionNameList.find(
        (item) => {
          return item.id === value;
        });

      this.targetConnectionSchemas = [];
      this.$http.get(
        "/dbswitch/admin/api/v1/connection/schemas/get/" + value
      ).then(res => {
        if (0 === res.data.code) {
          this.targetConnectionSchemas = res.data.data;
        } else {
          this.$message.error("查询目的端数据库的Schema失败," + res.data.message);
          this.targetConnectionSchemas = [];
        }
      });
    },
    handleGoBack () {
      this.$router.go(-1);
    },
  },
  created () {
    this.loadAssignmentDetail();
  },
}
</script>

<style scoped>
.el-card {
  width: 100%;
  height: 100%;
  overflow: auto;
}
.el-descriptions__body
  .el-descriptions__table
  .el-descriptions-row
  .el-descriptions-item__label {
  min-width: 20px;
  max-width: 60px;
}
</style>