<template>
  <div>
    <el-card>
      <el-tabs v-model="tabActiveTabName"
               type="border-card">
        <el-tab-pane label="元数据"
                     name="metadata">
          <div class="flex-between">
            <el-aside>
              <div class="select-datasource-container">
                <el-select placeholder="请选择数据源"
                           v-model="dataSourceId"
                           @change="loadTreeData">
                  <el-option v-for="(item,index) in connectionList"
                             :key="index"
                             :label="`[${item.id}]${item.name}`"
                             :value="item.id"></el-option>
                </el-select>
                <el-button type="primary"
                           size="mini"
                           :disabled="metadataLoading"
                           icon="el-icon-refresh"
                           @click="loadTreeData">刷新</el-button>
              </div>
              <el-scrollbar style="height: 800px;">
                <el-tree ref="metadataTree"
                         empty-text="请选择数据源后查看"
                         :indent=6
                         :data="treeData"
                         :props="props"
                         :load="loadTreeNode"
                         :expand-on-click-node="true"
                         :highlight-current="true"
                         :render-content="renderContent"
                         @node-click="handleNodeClick"
                         lazy>
                </el-tree>
              </el-scrollbar>
            </el-aside>
            <el-main class="metadata-container">
              当前表：<el-tag size="medium">{{currentNode.schemaName}} / {{currentNode.tableName}}</el-tag>
              <el-tabs v-model="metadataActiveTabName"
                       type="border-card">
                <el-tab-pane label="基本信息"
                             name="first">
                  <el-descriptions size="small"
                                   :column="2"
                                   colon
                                   border>
                    <el-descriptions-item label="模式名"><el-tag size="small">{{tableMeta.schemaName}}</el-tag></el-descriptions-item>
                    <el-descriptions-item label="表名称"><el-tag size="small">{{tableMeta.tableName}}</el-tag></el-descriptions-item>
                    <el-descriptions-item label="表类型"><el-tag size="small">{{tableMeta.type}}</el-tag></el-descriptions-item>
                    <el-descriptions-item label="表注释"><span class="long-text">{{tableMeta.remarks}}</span></el-descriptions-item>
                    <el-descriptions-item label="建表DDL">
                    </el-descriptions-item>
                  </el-descriptions>
                  <ace ref="ddlEditor"
                       :value="tableMeta.createSql"
                       @init="initEditor"
                       lang="sql"
                       height="500"
                       theme="chrome"
                       :options="editorOption"
                       width="100%">
                  </ace>
                </el-tab-pane>
                <el-tab-pane label="字段信息"
                             name="seconde">
                  <el-table :header-cell-style="{background:'#eef1f6',color:'#606266'}"
                            :data="tableMeta.columns"
                            size="small"
                            border
                            style="width: 100%">
                    <template slot="empty">
                      <span>单击点击左侧节点查看对应表的字段信息</span>
                    </template>
                    <el-table-column prop="fieldName"
                                     min-width="20%"
                                     show-overflow-tooltip
                                     label="名称">
                    </el-table-column>
                    <el-table-column prop="typeName"
                                     min-width="20%"
                                     label="数据类型">
                    </el-table-column>
                    <el-table-column prop="fieldType"
                                     min-width="7%"
                                     label="类型枚举">
                    </el-table-column>
                    <el-table-column prop="displaySize"
                                     min-width="7%"
                                     label="长度">
                    </el-table-column>
                    <el-table-column prop="precision"
                                     min-width="5%"
                                     label="精度">
                    </el-table-column>
                    <el-table-column prop="scale"
                                     min-width="5%"
                                     label="位数">
                    </el-table-column>
                    <el-table-column prop="isPrimaryKey"
                                     min-width="5%"
                                     label="主键">
                    </el-table-column>
                    <el-table-column prop="isAutoIncrement"
                                     min-width="5%"
                                     label="自增">
                    </el-table-column>
                    <el-table-column prop="isNullable"
                                     min-width="5%"
                                     label="可空">
                    </el-table-column>
                    <el-table-column prop="remarks"
                                     min-width="20%"
                                     show-overflow-tooltip
                                     label="注释">
                    </el-table-column>
                  </el-table>
                </el-tab-pane>
                <el-tab-pane label="索引信息"
                             name="third">
                  <el-table :header-cell-style="{background:'#eef1f6',color:'#606266'}"
                            :data="tableMeta.indexes"
                            size="small"
                            border
                            style="width: 100%">
                    <template slot="empty">
                      <span>单击点击左侧节点查看对应表的索引信息</span>
                    </template>
                    <el-table-column prop="indexType"
                                     min-width="20%"
                                     label="索引类型">
                    </el-table-column>
                    <el-table-column prop="indexName"
                                     min-width="20%"
                                     label="索引名称">
                    </el-table-column>
                    <el-table-column prop="indexFields"
                                     :formatter="formatIndexFields"
                                     show-overflow-tooltip
                                     min-width="60%"
                                     label="索引字段">
                    </el-table-column>
                  </el-table>
                </el-tab-pane>
                <el-tab-pane class="table-container-data-table"
                             label="取样数据"
                             name="fourth">
                  <el-table :header-cell-style="{background:'#eef1f6',color:'#606266'}"
                            :data="sampleData.rows"
                            border>
                    <template slot="empty">
                      <span>单击点击左侧节点查看对应表的取样数据</span>
                    </template>
                    <el-table-column v-for="(item,index) in sampleData.columns"
                                     :prop="item"
                                     :label="item"
                                     :key="index"
                                     show-overflow-tooltip>
                    </el-table-column>
                  </el-table>
                </el-tab-pane>
              </el-tabs>
            </el-main>
          </div>
        </el-tab-pane>
        <el-tab-pane label="SQL在线"
                     name="sqlQuery">
          <el-row :gutter=12
                  class="padding-row-stype">
            <el-col :span="6">
              <div class="sqlonline-select-suffix">
                <span class="text-label">数据源：</span>
                <el-select size="small"
                           placeholder="请选择数据源"
                           v-model="sqlDataSourceId">
                  <el-option v-for="(item,index) in connectionList"
                             :key="index"
                             :label="`[${item.id}]${item.name}`"
                             :value="item.id"></el-option>
                </el-select>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="sqlonline-select-suffix">
                <span class="text-label">最大记录数：</span>
                <el-select size="small"
                           placeholder="选择结果集MaxRow"
                           v-model="rsMaxRowCount">
                  <el-option v-for="(item,index) in maxRowCountList"
                             :key="index"
                             :label="item.name"
                             :value="item.id"></el-option>
                </el-select>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="sqlonline-select-suffix">
                <span class="text-label">编辑器高度：</span>
                <el-input-number v-model="editorHeightNum"
                                 size="small"
                                 :step="10"
                                 step-strictly></el-input-number>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="tool">
                <div class="item-button"
                     @click="runAll"><i class="el-icon-video-play"></i><span>执行</span></div>
                <div class="item-button"
                     @click="runSelected"><i class="el-icon-caret-right"></i><span>选中执行</span></div>
                <div class="item-button"
                     @click="formatSql"><i class="el-icon-postcard"></i><span>格式化</span></div>
              </div>
            </el-col>
          </el-row>
          <el-row class="padding-row-stype">
          </el-row>
          <el-row class="padding-row-stype">
            <div v-loading="sqlResultLoading"
                 :style="'height: ' + editorHeightNum + 'px'">
              <ace ref="sqlEditor"
                   @init="initEditor"
                   lang="sql"
                   width="100%"
                   height="100%"
                   theme="monokai"
                   :options="sqlEditorOption">
              </ace>
            </div>
          </el-row>
          <el-row class="padding-row-stype">
            <el-tabs v-model="activeResultTab"
                     tab-position="top"
                     type="border-card">
              <el-tab-pane label="信息"
                           name="0">
                <div v-for="(one,idx) in sqlExecuteResult.summaries"
                     :key="idx">
                  [SQL]: {{one.sql}}<br />{{one.summary}}<br /><br />
                </div>
              </el-tab-pane>
              <el-tab-pane v-for="(one,idx) in sqlExecuteResult.results"
                           :key="(idx+1)"
                           :label="'结果'+(idx+1)"
                           :name="''+(idx+1)">
                <el-table :header-cell-style="{background:'#eef1f6',color:'#606266','font-size': '12px'}"
                          style="width: 100%; max-height: 400px; overflow: auto;"
                          height="400px"
                          :data="one.rows"
                          border>
                  <template slot="empty">
                    <span>SQL结果为空</span>
                  </template>
                  <el-table-column v-for="(item,index) in one.columns"
                                   :prop="item.columnName"
                                   :key="index"
                                   show-overflow-tooltip>
                    <template slot="header">
                      {{item.columnName}}<br />({{item.columnType}})
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import urlencode from "urlencode";
import ace from 'vue2-ace-editor'
const sqlformatter = require("sql-formatter");

export default {
  components: {
    ace
  },
  data () {
    return {
      metadataLoading: false,
      props: {
        label: 'label',
        children: 'children',
        disabled: false,
        isLeaf: false
      },
      editorOption: {
        enableBasicAutocompletion: true,
        enableSnippets: true,
        enableLiveAutocompletion: true,
        showPrintMargin: false,
        fontSize: 16,
        readOnly: true
      },
      dataSourceId: null,
      connectionList: [],
      treeData: [],
      currentNode: {
        tableName: '-',
        schemaName: '-'
      },
      tabActiveTabName: 'metadata',
      metadataActiveTabName: 'first',
      tableMeta: {
        tableName: '-',
        schemaName: '-',
        remarks: '',
        type: '-',
        createSql: "",
        primaryKeys: [],
        columns: []
      },
      count: 1,
      sampleData: [],
      sqlDataSourceId: null,
      sqlEditorOption: {
        enableBasicAutocompletion: true,
        enableSnippets: true,
        enableLiveAutocompletion: true,
        showPrintMargin: false,
        showLineNumbers: true,
        tabSize: 6,
        fontSize: 18,
      },
      rsMaxRowCount: 200,
      maxRowCountList: [
        { id: 100, name: '100条' },
        { id: 200, name: '200条' },
        { id: 500, name: '500条' },
        { id: 1000, name: '1000条' },
        { id: 2000, name: '2000条' },
        { id: 5000, name: '5000条' },
        { id: 10000, name: '10000条' },
      ],
      editorHeightNum: 200,
      sqlResultLoading: false,
      sqlExecuteResult: {
        summaries: [],
        results: [],
      },
      activeResultTab: "0",
    };
  },
  methods: {
    initEditor (editor) {
      require('brace/ext/language_tools')
      // 设置语言
      require('brace/mode/sql')
      require('brace/snippets/sql')
      // 设置主题 按需加载
      require('brace/theme/monokai')
      require('brace/theme/chrome')
      require('brace/theme/crimson_editor')
    },
    loadConnections: function () {
      this.connectionList = [];
      this.$http({
        method: "GET",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/connection/list/name",
      }).then(
        res => {
          if (0 === res.data.code) {
            this.connectionList = res.data.data;
          } else {
            if (res.data.message) {
              alert("加载数据失败:" + res.data.message);
              this.connectionList = [];
            }
          }
        }
      );
    },
    loadTreeData: function () {
      if (this.dataSourceId && this.dataSourceId > 0) {
        this.treeData = []
        this.metadataLoading = true;
        setTimeout(() => {
          this.$http({
            method: "GET",
            url: "/dbswitch/admin/api/v1/connection/schemas/get/" + this.dataSourceId
          }).then(
            res => {
              this.metadataLoading = false;
              if (0 === res.data.code) {
                for (let element of res.data.data) {
                  let obj = new Object();
                  obj['dataSourceId'] = this.dataSourceId;
                  obj['label'] = element;
                  obj['parent'] = this.dataSourceId;
                  obj['value'] = element;
                  obj['hasChild'] = true;
                  obj['type'] = 'DATABASE';
                  this.treeData.push(obj);
                }
              } else {
                alert("加载失败，原因：" + res.data.message);
              }
            }
          );
        }, 500);
      }
    },
    loadTreeNode: function (node, resolve) {
      setTimeout(() => {
        if (node.level === 1) {
          let tableView = [
            {
              'dataSourceId': this.dataSourceId,
              'label': '表',
              'parent': this.dataSourceId,
              'value': node.label,
              'hasChild': true,
              'type': 'TABLE',
            },
            {
              'dataSourceId': this.dataSourceId,
              'label': '视图',
              'parent': this.dataSourceId,
              'value': node.label,
              'hasChild': true,
              'type': 'VIEW',
            }
          ]
          resolve(tableView);
        } else if (node.level === 2) {
          this.loadTablesList(resolve, this.dataSourceId, node.data.value, node.data.type)
        } else {
          resolve([]);
        }
      }, 500);
    },
    loadTablesList: function (resolve, dataSourceId, schema, type) {
      var tableType = 'VIEW' === type ? 'views' : 'tables'
      this.metadataLoading = true;
      this.$http({
        method: "GET",
        url: "/dbswitch/admin/api/v1/connection/" + tableType + "/get/" + dataSourceId + "?schema=" + urlencode(schema)
      }).then(
        res => {
          this.metadataLoading = false;
          if (0 === res.data.code) {
            let tableList = []
            for (let element of res.data.data) {
              let obj = new Object();
              obj['dataSourceId'] = dataSourceId;
              obj['label'] = element;
              obj['parent'] = dataSourceId;
              obj['value'] = schema;
              obj['hasChild'] = false;
              obj['type'] = type;
              tableList.push(obj);
            }
            return resolve(tableList);
          } else {
            this.$alert("加载失败，原因：" + res.data.message, '数据加载失败');
          }
        }
      );
    },
    renderContent (h, { node, data, store }) {
      // https://www.cnblogs.com/zhoushuang0426/p/11059989.html
      if (node.level === 1) {
        return (
          <div class="custom-tree-node">
            <i class="iconfont icon-shujuku1"></i>
            <el-tooltip class="item" effect="light" placement="left">
              <div slot="content">{node.label}</div>
              <span>{data.label}</span>
            </el-tooltip>
          </div>
        );
      } else if (node.level === 2) {
        var icon_pic = "iconfont icon-shitu_biaoge";
        if (data.type === 'VIEW') {
          icon_pic = "iconfont icon-viewList"
        }
        return (
          <div class="custom-tree-node">
            <i class={icon_pic}></i>
            <span>{data.label}</span>
          </div>
        );
      } else if (node.level === 3) {
        var icon_pic = "iconfont icon-shitu_biaoge";
        if (data.type === 'VIEW') {
          icon_pic = "iconfont icon-viewList"
        }
        return (
          <div class="custom-tree-node">
            <i class={icon_pic}></i>
            <el-tooltip class="item" effect="light" placement="left">
              <div slot="content">{node.label}</div>
              <span>{data.label}</span>
            </el-tooltip>
          </div>
        );
      } else {
        return (
          <div class="custom-tree-node">
            <i class="el-icon-set-up"></i>
            <el-tooltip class="item" effect="light" placement="left">
              <div slot="content">{data.type}</div>
              <span>{data.label}({data.type})</span>
            </el-tooltip>
          </div>
        );
      }
    },
    formatIndexFields (row, column) {
      let list = row.indexFields;
      let fields = list.map(
        item => {
          if (item.ascOrder === null) {
            return item.fieldName;
          } else if (item.ascOrder) {
            return item.fieldName + " ASC";
          } else {
            return item.fieldName + " DESC";
          }
        })
      return fields.join(";");
    },
    handleNodeClick (data) {
      var type = data.type;
      if (type === 'VIEW' || type === 'TABLE') {
        var datasourceId = data.dataSourceId;
        var schema = data.value;
        var table = data.label;
        if (!data.hasChild && datasourceId && schema && table) {
          this.tabActiveTabName = 'metadata';
          this.metadataActiveTabName = 'first';
          this.clearDataSet();
          this.getTableMeta(datasourceId, schema, table);
          this.getTableData(datasourceId, schema, table);
        }
      }
    },
    clearDataSet () {
      this.tableData = [];
      this.tableMeta = {
        tableName: '-',
        schemaName: '-',
        remarks: '',
        type: '-',
        createSql: "",
        primaryKeys: [],
        columns: []
      };
      this.sampleData = []
    },
    getTableMeta (id, schema, table) {
      this.$http({
        method: "GET",
        url: "/dbswitch/admin/api/v1/metadata/meta/table/" + id + "?schema=" + urlencode(schema) + "&table=" + urlencode(table)
      }).then(
        res => {
          if (0 === res.data.code) {
            //console.log("list4:" + JSON.stringify(res.data.data))
            this.tableMeta = res.data.data;
            this.currentNode.tableName = table;
            this.currentNode.schemaName = schema;
          } else {
            this.$alert("加载失败，原因：" + res.data.message, '数据加载失败');
          }
        }
      );
    },
    async getTableData (id, schema, table) {
      this.$http({
        method: "GET",
        url: "/dbswitch/admin/api/v1/metadata/data/table/" + id + "?schema=" + urlencode(schema) + "&table=" + urlencode(table)
      }).then(
        res => {
          if (0 === res.data.code) {
            this.sampleData = res.data.data;
            //console.log(this.sampleData)
          } else {
            this.$alert("加载失败，原因：" + res.data.message, '数据加载失败');
          }
        }
      );
    },
    loadColumnList (resolve, id, schema, table) {
      //console.log("id=" + id);
      this.$http({
        method: "GET",
        url: "/dbswitch/admin/api/v1/metadata/columns/" + id + "/1/0?schema=" + urlencode(schema) + "&table=" + urlencode(table)
      }).then(
        res => {
          if (0 === res.data.code) {
            //console.log("list3:" + JSON.stringify(res.data.data))
            res.data.data.forEach(function (element) {
              element['label'] = element.fieldName.name;
              element['parent'] = schema;
              element['value'] = element.fieldName.value;
              element['hasChild'] = false;
              element['children'] = 'child';
            });
            this.tableData = res.data.data;
            return resolve([]);
          } else {
            this.$alert("加载失败，原因：" + res.data.message, '数据加载失败');
            this.clearDataSet();
          }
        }
      );
    },
    formatSql: function () {
      if (this.sqlResultLoading === true) {
        return
      }
      let editor = this.$refs.sqlEditor.editor;
      let sqlcontent = editor.getSelectedText();
      if (sqlcontent && sqlcontent.length > 0) {
        let formatSql = sqlformatter.format(sqlcontent)
        editor.session.replace(editor.getSelectionRange(), formatSql);
      } else {
        sqlcontent = editor.getValue();
        if (!sqlcontent || sqlcontent.length === 0) {
          alert("SQL文本内容为空");
          return
        }
        editor.setValue(sqlformatter.format(editor.getValue()));
      }
    },
    runAll: function () {
      let editor = this.$refs.sqlEditor.editor;
      let sqlcontent = editor.getValue();
      if (!sqlcontent || 0 === sqlcontent.length) {
        alert("SQL文本内容为空");
        return
      }
      this.executeSqlScript(sqlcontent)
    },
    runSelected: function () {
      let editor = this.$refs.sqlEditor.editor;
      let sqlcontent = editor.getSelectedText();
      if (!sqlcontent || 0 === sqlcontent.length) {
        alert("请首先选择SQL文本内容");
        return
      }
      this.executeSqlScript(sqlcontent)
    },
    executeSqlScript: function (sqlScript) {
      if (this.sqlResultLoading === true) {
        alert("已有一个查询正在进行中");
        return
      }
      if (!this.sqlDataSourceId || this.dataSourceId < 0) {
        alert("请首先选择一个数据源来");
        return
      }
      this.sqlResultLoading = true;
      this.sqlExecuteResult = {
        summaries: [],
        results: [],
      };
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/metadata/data/sql/" + this.sqlDataSourceId,
        data: JSON.stringify({
          script: sqlScript,
          page: 1,
          size: this.rsMaxRowCount
        })
      }).then(res => {
        this.sqlResultLoading = false;
        if (0 === res.data.code) {
          this.sqlExecuteResult = res.data.data;
          this.activeResultTab = "0";
        } else {
          alert("SQL执行报错:" + res.data.message);
        }
      }
      );
    },
  },
  created () {
    this.loadConnections();
    this.loadTreeData();
  },
}
</script>

<style scoped>
.el-card {
  width: 100%;
  height: 100%;
  min-height: 200px;
}

.el-message {
  width: 100%;
  height: 100%;
  overflow: auto;
}
.flex-between {
  display: flex;
}
.tree-container {
  min-width: calc(25%);
  position: relative;
  cursor: default;
  color: black;
  font-size: 14px;
  background-size: 16px;
}
.custom-tree-node {
  font-size: 8px;
  background-size: 16px;
}
.el-scrollbar {
  overflow-x: auto;
}
.el-select {
  display: inline;
}
.tree-container .tree {
  overflow: auto;
}
.metadata-container {
  padding: 4px;
}
.table-container {
  width: 100%;
  border: darkblue;
}
.table-container-data-table {
  height: 90%;
  overflow-y: auto;
  overflow-x: hidden;
}
.long-text {
  display: -webkit-box;
  width: 300px; /* 定义长文本的显示宽度 */
  white-space: normal !important;
  overflow: hidden; /* 超出部分隐藏 */
  text-overflow: ellipsis; /* 显示省略号 */
  text-align: left;
}
el-tabs--border-card > .el-tabs__header .el-tabs__item {
  margin-left: 8px;
  border: none;
  border-radius: 8px 8px 0 0;
  background-color: #f3f7fe;
  padding: 4px 20px;
  color: #0065d5;
  line-height: 22px;
  height: 30px;
}
.el-tabs--border-card > .el-tabs__header .el-tabs__item.is-active {
  background-color: #0065d5;
  color: #ffffff;
}
.sqlonline-select-suffix {
  display: flex;
  justify-content: flex-start;
  align-items: center; /* 垂直居中 */
}
.text-label {
  font-size: 11px;
  font-weight: 700;
}
.select-datasource-container {
  display: flex;
}
.tool {
  display: flex;
  justify-content: flex-end;
}
.tool .item-button {
  display: inline-block;
  font-size: 20px;
  color: #009966;
  margin: 0 20px 0 0;
  line-height: 26px;
  cursor: pointer;
}
.tool .item-button span {
  color: #000;
  font-size: 16px;
}
.padding-row-stype {
  padding: 5px;
}
</style>
