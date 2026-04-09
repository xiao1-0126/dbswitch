<template>
  <el-dialog
    :title="'建表语句预览与编辑 (共 ' + tableList.length + ' 张表)'"
    :visible.sync="visible"
    width="92%"
    top="3vh"
    :close-on-click-modal="false"
    :before-close="handleClose"
  >
    <el-alert title="说明信息"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 12px"
    />
       <p>(1) 点击左侧表名即可按需加载并编辑该表的 CREATE TABLE 建表语句。</p>
       <p>(2) 被修改了得表的 CREATE TABLE 建表语句要符合数据库语法，否则会导致任务执行失败。</p>
       <p>(3) 未加载或未修改的表将使用系统自动生成的默认语句。</p>
       <p>(4) 不要修改建表语句中的表名与字段名，否则可能会导致字段名映射错误导致数据同步写入失败。</p>
    </el-alert>
    <el-row :gutter="12" style="height: 520px">
      <!-- 左侧：表名列表 -->
      <el-col :span="7" style="height: 100%; display: flex; flex-direction: column">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px">
          <span style="font-size:13px;font-weight:bold;color:#303133">表名列表</span>
          <el-button size="mini" type="text" @click="handleResetAll">重置全部</el-button>
        </div>
        <el-input
          v-model="tableSearch"
          size="mini"
          placeholder="搜索表名..."
          prefix-icon="el-icon-search"
          clearable
          style="margin-bottom:8px"
        />
        <div style="flex:1;overflow-y:auto;border:1px solid #EBEEF5;border-radius:4px">
          <div
            v-for="item in filteredTableList"
            :key="item.sourceTableName"
            class="table-list-item"
            :class="{
              'is-active': selectedTable && selectedTable.sourceTableName === item.sourceTableName,
              'is-modified': isModified(item),
              'is-loading': item.loading
            }"
            @click="handleSelectTable(item)"
          >
            <div style="display:flex;align-items:center;justify-content:space-between">
              <div style="overflow:hidden">
                <div class="table-name-text" :title="item.targetTableName">{{ item.targetTableName }}</div>
                <div class="source-name-text" :title="item.sourceTableName">{{ item.sourceTableName }}</div>
              </div>
              <div style="flex-shrink:0;margin-left:4px">
                <el-tag v-if="item.loading" size="mini" type="info">加载中</el-tag>
                <el-tag v-else-if="isModified(item)" size="mini" type="warning" effect="dark">已修改</el-tag>
                <el-tag v-else-if="item.ddlLoaded" size="mini" type="success">已预览</el-tag>
              </div>
            </div>
          </div>
          <div v-if="filteredTableList.length === 0" style="padding:20px;text-align:center;color:#909399;font-size:13px">
            无匹配表
          </div>
        </div>
      </el-col>

      <!-- 右侧：DDL编辑区 -->
      <el-col :span="17" style="height: 100%; display: flex; flex-direction: column">
        <div v-if="!selectedTable" class="ddl-placeholder">
          <i class="el-icon-document" style="font-size:48px;color:#C0C4CC;margin-bottom:12px"></i>
          <p style="color:#909399;font-size:14px">请在左侧点击一张表查看并编辑其建表语句</p>
        </div>

        <div v-else style="height:100%;display:flex;flex-direction:column">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px">
            <div>
              <span style="font-weight:bold;color:#303133;font-size:14px">{{ selectedTable.targetTableName }}</span>
              <span style="color:#909399;font-size:12px;margin-left:8px">← {{ selectedTable.sourceTableName }}</span>
            </div>
            <div style="display:flex;align-items:center;gap:8px">
              <el-tag v-if="selectedTable.loading" size="small" type="info">加载中...</el-tag>
              <el-tag v-else-if="isModified(selectedTable)" size="small" type="warning" effect="dark">已修改</el-tag>
              <el-tag v-else-if="selectedTable.ddlLoaded" size="small" type="success">默认</el-tag>
              <el-button
                v-if="selectedTable.ddlLoaded && isModified(selectedTable)"
                size="mini"
                icon="el-icon-refresh-right"
                @click="handleResetRow(selectedTable)"
              >重置</el-button>
            </div>
          </div>

          <!-- 加载中骨架 -->
          <div v-if="selectedTable.loading" class="ddl-loading">
            <i class="el-icon-loading" style="font-size:24px;color:#409EFF"></i>
            <p style="margin-top:8px;color:#606266">正在加载 {{ selectedTable.targetTableName }} 的建表语句...</p>
          </div>

          <!-- DDL编辑框 -->
          <el-input
            v-else-if="selectedTable.ddlLoaded"
            v-model="selectedTable.ddlSql"
            type="textarea"
            :rows="18"
            resize="vertical"
            placeholder="使用系统自动生成的默认DDL..."
            :style="{ fontFamily: 'Consolas, Monaco, monospace', fontSize: '12px', flex: 1 }"
            @change="handleDdlChange(selectedTable)"
          />

          <!-- 加载失败 -->
          <div v-else-if="selectedTable.loadError" class="ddl-loading">
            <i class="el-icon-warning" style="font-size:24px;color:#F56C6C"></i>
            <p style="margin-top:8px;color:#F56C6C">加载失败: {{ selectedTable.loadError }}</p>
            <el-button size="small" style="margin-top:8px" @click="loadSingleDdl(selectedTable)">重试</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <div slot="footer" class="dialog-footer">
      <span style="font-size:12px;color:#909399;margin-right:16px">
        已修改 {{ modifiedCount }} 张表
      </span>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确认保存</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'DdlPreviewDialog',
  props: {
    dialogVisible: {
      type: Boolean,
      default: false
    },
    // 后端预览接口调用所需的请求参数（由父组件传入）
    previewRequestParams: {
      type: Object,
      default: function () { return {} }
    }
  },
  data () {
    return {
      visible: this.dialogVisible,
      // 表列表，每项结构: { sourceTableName, targetTableName, ddlLoaded, loading, loadError, ddlSql, originalDdl }
      tableList: [],
      // 用户在编辑器上显示的选中表
      selectedTable: null,
      // 搜索过滤
      tableSearch: ''
    }
  },
  computed: {
    filteredTableList () {
      var q = (this.tableSearch || '').trim().toLowerCase()
      if (!q) return this.tableList
      return this.tableList.filter(function (item) {
        return item.targetTableName.toLowerCase().indexOf(q) >= 0 ||
          item.sourceTableName.toLowerCase().indexOf(q) >= 0
      })
    },
    modifiedCount () {
      return this.tableList.filter(item => this.isModified(item)).length
    }
  },
  watch: {
    dialogVisible (val) {
      this.visible = val
    },
    visible (val) {
      this.$emit('update:dialogVisible', val)
    }
  },
  methods: {
    /**
     * 初始化表列表（由父组件调用）
     * @param {Array} tableInfoList  - [{sourceTableName, targetTableName}]
     * @param {Object} existingMap   - 已存在的自定义DDL映射 {targetTableName: ddl}
     */
    loadTableList (tableInfoList, existingMap) {
      this.tableList = []
      this.selectedTable = null
      this.tableSearch = ''
      if (!tableInfoList || tableInfoList.length === 0) return

      var existing = existingMap || {}
      var self = this
      tableInfoList.forEach(function (info) {
        var customDdl = existing[info.targetTableName] || null
        var row = {
          sourceTableName: info.sourceTableName,
          targetTableName: info.targetTableName,
          ddlLoaded: false,
          loading: false,
          loadError: null,
          ddlSql: customDdl,      // 若已有用户自定义DDL则提前填入
          originalDdl: null       // 待首次加载时记录系统生成的原始DDL
        }
        // 若存在已保存的自定义DDL，视为已修改状态（懒加载原始DDL时会再刷新 originalDdl）
        self.tableList.push(row)
      })
    },

    /**
     * 用户点击左侧表名
     */
    handleSelectTable (item) {
      this.selectedTable = item
      if (!item.ddlLoaded && !item.loading) {
        this.loadSingleDdl(item)
      }
    },

    /**
     * 按需从后端加载单张表的DDL
     */
    loadSingleDdl (item) {
      var params = this.previewRequestParams
      if (!params || !params.sourceConnectionId) {
        item.loadError = '缺少请求参数，请关闭弹窗后重试'
        return
      }

      this.$set(item, 'loading', true)
      this.$set(item, 'loadError', null)

      var self = this
      this.$http({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        url: '/dbswitch/admin/api/v1/assignment/preview-ddl/single',
        data: JSON.stringify({
          sourceConnectionId: params.sourceConnectionId,
          sourceSchema: params.sourceSchema,
          targetConnectionId: params.targetConnectionId,
          targetSchema: params.targetSchema,
          sourceTable: item.sourceTableName,
          tableNameMapper: params.tableNameMapper || [],
          columnNameMapper: params.columnNameMapper || [],
          tableNameCase: params.tableNameCase || 'NONE',
          columnNameCase: params.columnNameCase || 'NONE',
          targetAutoIncrement: params.targetAutoIncrement || false
        })
      }).then(function (res) {
        self.$set(item, 'loading', false)
        if (res.data.code === 0) {
          var data = res.data.data
          // 记录系统原始DDL
          self.$set(item, 'originalDdl', data.ddlSql)
          self.$set(item, 'ddlLoaded', true)
          // 若用户之前已保存过自定义DDL则保留，否则用系统生成的
          if (item.ddlSql === null || item.ddlSql === undefined) {
            self.$set(item, 'ddlSql', data.ddlSql)
          }
        } else {
          self.$set(item, 'loadError', res.data.message || '未知错误')
        }
      }).catch(function (err) {
        self.$set(item, 'loading', false)
        self.$set(item, 'loadError', '请求异常: ' + (err.message || err))
      })
    },

    isModified (item) {
      // 尚未加载原始DDL时，若已有用户自定义DDL则视为已修改
      if (!item.ddlLoaded) {
        return !!(item.ddlSql && item.ddlSql.trim())
      }
      if (!item.ddlSql || !item.originalDdl) return false
      return item.ddlSql.trim() !== item.originalDdl.trim()
    },

    handleDdlChange (item) {
      // 触发响应式更新（Vue 2 已监听对象属性变化，无需额外操作）
    },

    handleResetRow (item) {
      if (item && item.originalDdl) {
        this.$set(item, 'ddlSql', item.originalDdl)
      }
    },

    handleResetAll () {
      var self = this
      this.tableList.forEach(function (item) {
        if (item.ddlLoaded && item.originalDdl) {
          self.$set(item, 'ddlSql', item.originalDdl)
        } else if (!item.ddlLoaded) {
          // 未加载的，直接清空已保存的自定义DDL
          self.$set(item, 'ddlSql', null)
        }
      })
    },

    handleCancel () {
      this.visible = false
    },

    handleClose (done) {
      done()
    },

    handleConfirm () {
      var customDdlMap = {}
      var modifiedCount = 0
      var self = this
      this.tableList.forEach(function (item) {
        if (self.isModified(item) && item.ddlSql && item.ddlSql.trim()) {
          customDdlMap[item.targetTableName] = item.ddlSql.trim()
          modifiedCount++
        }
      })
      this.$emit('confirm', customDdlMap, modifiedCount)
      this.visible = false
    }
  }
}
</script>

<style scoped>
.dialog-footer {
  text-align: right;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.table-list-item {
  padding: 8px 10px;
  cursor: pointer;
  border-bottom: 1px solid #F2F6FC;
  transition: background 0.15s;
  user-select: none;
}

.table-list-item:hover {
  background-color: #ECF5FF;
}

.table-list-item.is-active {
  background-color: #D9ECFF;
  border-left: 3px solid #409EFF;
}

.table-list-item.is-modified {
  border-left: 3px solid #E6A23C;
}

.table-list-item.is-active.is-modified {
  background-color: #FDF6EC;
  border-left: 3px solid #E6A23C;
}

.table-name-text {
  font-size: 13px;
  font-weight: bold;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.source-name-text {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
  margin-top: 2px;
}

.ddl-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed #DCDFE6;
  border-radius: 4px;
  color: #909399;
}

.ddl-loading {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
}

/* SQL文本框样式 */
/deep/ .el-textarea__inner {
  font-family: Consolas, Monaco, "Courier New", monospace !important;
  font-size: 12px !important;
  line-height: 1.5 !important;
  background-color: #fafafa;
  color: #303133;
  height: 100%;
}
</style>
