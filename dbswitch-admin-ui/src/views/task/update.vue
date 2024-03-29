<template>
  <el-card>
    <el-steps :active="active"
              finish-status="success">
      <el-step title="基本信息配置" />
      <el-step title="同步源端配置" />
      <el-step title="目标端配置" />
      <el-step title="映射转换配置" />
      <el-step title="配置确认提交" />
    </el-steps>

    <el-form :model="updateform"
             status-icon
             :rules="rules"
             ref="updateform">

      <div v-show="active == 1">
        <el-form-item label="名称"
                      label-width="240px"
                      :required=true
                      prop="name"
                      style="width:65%">
          <el-input v-model="updateform.name"
                    auto-complete="off"></el-input>
        </el-form-item>
        <el-form-item label="描述"
                      label-width="240px"
                      prop="description"
                      style="width:65%">
          <el-input v-model="updateform.description"
                    type="textarea"
                    :rows="3"
                    auto-complete="off"></el-input>
        </el-form-item>
        <el-form-item label="调度方式"
                      label-width="240px"
                      :required=true
                      prop="scheduleMode"
                      style="width:65%">
          <el-select v-model="updateform.scheduleMode">
            <el-option label="手动调度"
                       value="MANUAL"></el-option>
            <el-option label="系统调度"
                       value="SYSTEM_SCHEDULED"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="Cron表达式"
                      label-width="240px"
                      style="width:65%"
                      v-if="updateform.scheduleMode=='SYSTEM_SCHEDULED'">
          <el-tooltip placement="top">
            <div slot="content">
              执行周期为CRON表达式，即可以选择以下内置的周期，也可以自行输入或粘贴合法的CRON表达式(最小间隔时间为2分钟)。
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-select v-model="updateform.cronExpression"
                     filterable
                     allow-create>
            <el-option label="每5分钟执行1次"
                       value="0 0/5 * * * ? *"></el-option>
            <el-option label="每30分钟执行1次"
                       value="0 0/30 * * * ? *"></el-option>
            <el-option label="每1小时执行1次"
                       value="0 0 0/1 * * ? *"></el-option>
            <el-option label="每2小时执行1次"
                       value="0 0 0/2 * * ? *"></el-option>
            <el-option label="每8小时执行1次"
                       value="0 0 0/8 * * ? *"></el-option>
            <el-option label="每12小时执行1次"
                       value="0 0 0/12 * * ? *"></el-option>
            <el-option label="每日0时执行1次"
                       value="0 0 0 1/1 * ? *"></el-option>
          </el-select>
        </el-form-item>
      </div>
      <div v-show="active == 2">
        <el-form-item label="源端数据源"
                      label-width="240px"
                      :required=true
                      prop="sourceConnectionId"
                      style="width:65%">
          <el-select v-model="updateform.sourceConnectionId"
                     @change="selectChangedSourceConnection"
                     placeholder="请选择">
            <el-option v-for="(item,index) in connectionNameList"
                       :key="index"
                       :label="`[${item.id}]${item.name}`"
                       :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="源端模式名"
                      label-width="240px"
                      :required=true
                      prop="sourceSchema"
                      style="width:65%">
          <el-select v-model="updateform.sourceSchema"
                     filterable
                     @change="selectUpdateChangedSourceSchema"
                     placeholder="请选择">
            <el-option v-for="(item,index) in sourceConnectionSchemas"
                       :key="index"
                       :label="item"
                       :value="item"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="源端表类型"
                      label-width="240px"
                      :required=true
                      prop="tableType"
                      style="width:65%">
          <el-select placeholder="请选择表类型"
                     @change="selectUpdateChangedTableType"
                     v-model="updateform.tableType">
            <el-option label="物理表"
                       value="TABLE"></el-option>
            <el-option label="视图表"
                       value="VIEW"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="配置方式"
                      label-width="240px"
                      :required=true
                      prop="includeOrExclude"
                      style="width:65%">
          <el-select placeholder="请选择配置方式"
                     v-model="updateform.includeOrExclude">
            <el-option label="包含表"
                       value="INCLUDE"></el-option>
            <el-option label="排除表"
                       value="EXCLUDE"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="表名配置"
                      label-width="240px"
                      :required=false
                      prop="sourceTables"
                      style="width:65%">
          <el-tooltip placement="top">
            <div slot="content">
              当为包含表时，选择所要精确包含的表名，如果不选则代表选择所有；当为排除表时，选择索要精确排除的表名。
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-select placeholder="请选择表名"
                     multiple
                     filterable
                     v-model="updateform.sourceTables">
            <el-option v-for="(item,index) in sourceSchemaTables"
                       :key="index"
                       :label="item"
                       :value="item"></el-option>
          </el-select>
        </el-form-item>
      </div>
      <div v-show="active == 3">
        <el-form-item label="目的端数据源"
                      label-width="240px"
                      :required=true
                      prop="targetConnectionId"
                      style="width:65%">
          <el-select v-model="updateform.targetConnectionId"
                     @change="selectChangedTargetConnection"
                     placeholder="请选择">
            <el-option v-for="(item,index) in connectionNameList"
                       :key="index"
                       :label="`[${item.id}]${item.name}`"
                       :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目的端模式名"
                      label-width="240px"
                      :required=true
                      prop="targetSchema"
                      style="width:65%">
          <el-select v-model="updateform.targetSchema"
                     placeholder="请选择">
            <el-option v-for="(item,index) in targetConnectionSchemas"
                       :key="index"
                       :label="item"
                       :value="item"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="自动同步模式"
                      label-width="240px"
                      :required=true
                      prop="autoSyncMode"
                      style="width:65%">
          <span slot="label">
            <span style="color: red"><strong>自动同步模式</strong> </span>
          </span>
          <el-tooltip placement="top">
            <div slot="content">
              <p>目标端建表并同步数据：首次在目标的自动建表(存在重命表时会删除重建)并执行数据加载同步操作，再次执行时会根据是否有主键进行变化量同步；</p>
              <p>目标端只创建物理表: 每次执行时，只在目标端自动建表，存在重名表时会删除后重建；</p>
              <p>目标端只同步表里数据：每次执行时，目标端需要存在符合映射规则的物理表，最迟需要在执行任务前已经存在目标表；该选项通<br />
                常适用于两端表结构一致时(或目标端字段包含源端所有的字段且字段数据类型一致)的数据同步场景</p>
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-select v-model="updateform.autoSyncMode">
            <el-option label='目标端建表并同步数据'
                       :value=2></el-option>
            <el-option label='目标端只创建物理表'
                       :value=1></el-option>
            <el-option label='目标端只同步表里数据'
                       :value=0></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="建表字段自增"
                      label-width="240px"
                      :required=true
                      v-if=" updateform.autoSyncMode!==0 "
                      prop="targetAutoIncrement"
                      style="width:65%">
          <el-tooltip placement="top">
            <div slot="content">
              创建表时是否自动支持字段的自增；只有使用自动建表才会生效，不过前提需要两端的数据库表建表时支持指定自增字段，默认为false。
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-select v-model="updateform.targetAutoIncrement">
            <el-option label='是'
                       :value=true></el-option>
            <el-option label='否'
                       :value=false></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="表名大小写转换"
                      label-width="240px"
                      :required=true
                      prop="tableNameCase"
                      style="width:45%">
          <el-tooltip placement="top">
            <div slot="content">
              表名大小写转换说明：先使用下面的表名映射，然后再使用这里的大小写转换，对支持大小写敏感的数据库类型有效。
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-select v-model="updateform.tableNameCase">
            <el-option label='无转换'
                       value='NONE'></el-option>
            <el-option label='转大写'
                       value='UPPER'></el-option>
            <el-option label='转小写'
                       value='LOWER'></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="列名大小写转换"
                      label-width="240px"
                      :required=true
                      prop="columnNameCase"
                      style="width:45%">
          <el-tooltip placement="top">
            <div slot="content">
              列名大小写转换说明：先使用下面的列名映射，然后再使用这里的大小写转换，对支持大小写敏感的数据库类型有效。
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-select v-model="updateform.columnNameCase">
            <el-option label='无转换'
                       value='NONE'></el-option>
            <el-option label='转大写'
                       value='UPPER'></el-option>
            <el-option label='转小写'
                       value='LOWER'></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="数据批次大小"
                      label-width="240px"
                      :required=true
                      v-if=" updateform.autoSyncMode!==1 "
                      prop="batchSize"
                      style="width:65%">
          <el-tooltip placement="top">
            <div slot="content">
              数据同步时单个批次处理的行记录总数，该值越大越占用内存空间。建议：小字段表设置为10000或20000，大字段表设置为100或500
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-select v-model="updateform.batchSize">
            <el-option label=100
                       :value=100></el-option>
            <el-option label=500
                       :value=500></el-option>
            <el-option label=1000
                       :value=1000></el-option>
            <el-option label=5000
                       :value=5000></el-option>
            <el-option label=10000
                       :value=10000></el-option>
            <el-option label=20000
                       :value=20000></el-option>
          </el-select>
        </el-form-item>
        </el-form-item>
        <el-form-item label="通道队列大小"
                      label-width="240px"
                      :required=true
                      v-if=" updateform.autoSyncMode!==1 "
                      prop="channelSize"
                      style="width:65%">
          <el-tooltip placement="top">
            <div slot="content">
              数据同步时缓存数据的通道队列大小，该值越大越占用内存空间。当源库读取快目标库写入慢时，缓存在内存中的数据最大占用空间 = 行记录大小 × 数据批次大小 × 通道队列大小 。
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-select v-model="updateform.channelSize">
            <el-option label=10
                       :value=10></el-option>
            <el-option label=20
                       :value=20></el-option>
            <el-option label=40
                       :value=40></el-option>
            <el-option label=60
                       :value=60></el-option>
            <el-option label=80
                       :value=80></el-option>
            <el-option label=100
                       :value=100></el-option>
            <el-option label=500
                       :value=500></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="同步操作方法"
                      label-width="240px"
                      :required=true
                      v-if=" updateform.autoSyncMode!==1 "
                      prop="targetSyncOption"
                      style="width:65%">
          <el-tooltip placement="top">
            <div slot="content">
              数据同步时包括增删改操作，这里选择配置执行INSERT、UPDATE、DELETE操作类型的方法，对首次数据加载无效，只对数据同步有效。
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-select v-model="updateform.targetSyncOption">
            <el-option label='只同步INSERT操作'
                       value='ONLY_INSERT'></el-option>
            <el-option label='只同步UPDATE操作'
                       value='ONLY_UPDATE'></el-option>
            <el-option label='只同步INSERT和UPDATE'
                       value='INSERT_UPDATE'></el-option>
            <el-option label='只同步DELETE操作'
                       value='ONLY_DELETE'></el-option>
            <el-option label='只同步INSERT和DELETE'
                       value='INSERT_DELETE'></el-option>
            <el-option label='只同步UPDATE和DELETE'
                       value='UPDATE_DELETE'></el-option>
            <el-option label='执行所有的同步操作'
                       value='INSERT_UPDATE_DELETE'></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="同步前置执行SQL脚本"
                      label-width="240px"
                      v-if=" updateform.autoSyncMode!==1 "
                      prop="beforeSqlScripts"
                      style="width:65%">
          <el-tooltip placement="top">
            <div slot="content">
              数据同步写入目标断数据库前执行的SQL，多个SQL间以英文逗号分隔。使用场景如：MySQL数据库关闭外键约束 SET FOREIGN_KEY_CHECKS = 0
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-input v-model="updateform.beforeSqlScripts"
                    type="textarea"
                    :rows="3"
                    auto-complete="off"></el-input>
        </el-form-item>
        <el-form-item label="同步后置执行SQL脚本"
                      label-width="240px"
                      v-if=" updateform.autoSyncMode!==1 "
                      prop="afterSqlScripts"
                      style="width:65%">
          <el-tooltip placement="top">
            <div slot="content">
              数据同步写入目标断数据库后执行的SQL，多个SQL间以英文逗号分隔。使用场景如：MySQL数据库恢复外键约束 SET FOREIGN_KEY_CHECKS = 1
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
          <el-input v-model="updateform.afterSqlScripts"
                    type="textarea"
                    :rows="3"
                    auto-complete="off"></el-input>
        </el-form-item>
      </div>
      <div v-show="active == 4">
        <el-alert title="说明信息"
                  type="success">
          <p>(1) 当表名映射规则记录为空时，代表目标表名与源表名的名称相同;</p>
          <p>(2) 当字段名映射规则记录为空时，代表目标表的字段名与源表名的字段名相同</p>
          <p>(3) 在字段名映射规则中，如果目标字段名为空（未填写），则代表剔除该字段（不能是主键）的同步</p>
        </el-alert>
        <el-button type="primary"
                   @click="addTableNameMapperListRow()"
                   round>添加表名映射</el-button>
        <el-button type="warning"
                   @click="previewTableNameMapList()"
                   round>预览表名映射</el-button>
        <el-table :data="updateform.tableNameMapper"
                  size="mini"
                  border
                  height="200"
                  style="width:90%;margin-top: 15px;">
          <template slot="empty">
            <span>请点击"添加表名映射"按钮添加表名映射关系记录</span>
          </template>
          <el-table-column label="表名匹配的正则名"
                           width="320">
            <template slot-scope="scope">
              <el-input v-model="scope.row.fromPattern"
                        type="string"> </el-input>
            </template>
          </el-table-column>
          <el-table-column label="替换的目标值"
                           width="320">
            <template slot-scope="scope">
              <el-input v-model="scope.row.toValue"
                        type="string"></el-input>
            </template>
          </el-table-column>
          <el-table-column label="操作"
                           width="220">
            <template slot-scope="scope">
              <el-button size="mini"
                         type="danger"
                         @click="deleteTableNameMapperListItem(scope.$index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary"
                   @click="addColumnNameMapperListRow()"
                   round>添加字段名映射</el-button>
        <el-button type="warning"
                   @click="previewColumnNameMapList()"
                   round>预览字段名映射</el-button>
        <el-table :data="datas = updateform.columnNameMapper"
                  size="mini"
                  border
                  height="200"
                  style="width:90%;margin-top: 15px;">
          <template slot="empty">
            <span>请点击"添加字段名映射"按钮添加字段名映射关系记录</span>
          </template>
          <el-table-column label="字段名匹配的正则名"
                           width="320">
            <template slot-scope="scope">
              <el-input v-model="scope.row.fromPattern"
                        type="string"> </el-input>
            </template>
          </el-table-column>
          <el-table-column label="替换的目标值"
                           width="320">
            <template slot-scope="scope">
              <el-input v-model="scope.row.toValue"
                        type="string"></el-input>
            </template>
          </el-table-column>
          <el-table-column label="操作"
                           width="220">
            <template slot-scope="scope">
              <el-button size="mini"
                         type="danger"
                         @click="deleteColumnNameMapperListItem(scope.$index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-show="active == 5">
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
          <el-descriptions-item label="表名大小写转换">
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
          <el-descriptions-item label="列名大小写转换">
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
          <el-descriptions-item label="数据批次大小"
                                v-if=" updateform.autoSyncMode!==1 ">{{updateform.batchSize}}</el-descriptions-item>
          <el-descriptions-item label="通道队列大小"
                                v-if=" updateform.autoSyncMode!==1 ">{{updateform.channelSize}}</el-descriptions-item>
          <el-descriptions-item label="同步操作方法"
                                v-if=" updateform.autoSyncMode!==1 ">{{updateform.targetSyncOption}}</el-descriptions-item>
          <el-descriptions-item label="同步前置执行SQL脚本"
                                v-if=" updateform.autoSyncMode!==1 ">
            <span v-show="!updateform.beforeSqlScripts || updateform.beforeSqlScripts.length==0">[SQL脚本内容为空]</span>
            <span v-show="updateform.beforeSqlScripts && updateform.beforeSqlScripts.length>0">{{updateform.beforeSqlScripts}}</span>
          </el-descriptions-item>
          <el-descriptions-item label="同步后置执行SQL脚本"
                                v-if=" updateform.autoSyncMode!==1 ">
            <span v-show="!updateform.afterSqlScripts || updateform.afterSqlScripts.length==0">[SQL脚本内容为空]</span>
            <span v-show="updateform.afterSqlScripts && updateform.afterSqlScripts.length>0">{{updateform.afterSqlScripts}}</span>
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
      </div>

    </el-form>

    <el-button round
               v-if="active > 1"
               style="margin-top: 12px"
               @click="pre">
      上一步
    </el-button>
    <el-button round
               @click="next"
               v-if="active > 0 && active < 5">
      下一步
    </el-button>
    <el-button round
               @click="handleSave"
               v-if="active == 5 && !can_go_back">
      提交
    </el-button>
    <el-button round
               @click="handleGoBack"
               v-if="active == 5 && can_go_back">
      返回
    </el-button>

    <el-dialog v-if="active == 4"
               title="查看表名映射关系"
               :visible.sync="tableNameMapperDialogVisible"
               :showClose="false"
               :before-close="handleClose">
      <el-table :header-cell-style="{background:'#eef1f6',color:'#606266'}"
                :data="tableNamesMapperData"
                size="mini"
                border>
        <el-table-column prop="originalName"
                         label="源端表名"
                         min-width="20%"></el-table-column>
        <el-table-column prop="targetName"
                         label="目标表名"
                         min-width="20%"></el-table-column>
      </el-table>
      <div slot="footer"
           class="dialog-footer">
        <el-button @click="tableNameMapperDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <el-dialog v-if="active == 4"
               title="查看字段映射关系"
               :visible.sync="columnNameMapperDialogVisible"
               :showClose="false"
               :before-close="handleClose">
      <el-select @change="queryPreviewColumnNameMapperList"
                 v-model="preiveTableName"
                 placeholder="请选择">
        <el-option v-for="(item,index) in preiveSeeTableNameList"
                   :key="index"
                   :label="item"
                   :value="item"></el-option>
      </el-select>
      <el-table :header-cell-style="{background:'#eef1f6',color:'#606266'}"
                :data="columnNamesMapperData"
                size="mini"
                border>
        <el-table-column prop="originalName"
                         label="原始字段名"
                         min-width="20%"></el-table-column>
        <el-table-column prop="targetName"
                         label="目标表字段名"
                         min-width="20%"></el-table-column>
      </el-table>
      <div slot="footer"
           class="dialog-footer">
        <el-button @click="columnNameMapperDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>

  </el-card>
</template>

<script>
export default {

  data () {
    return {
      cronPopover: false,
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
        targetAutoIncrement: false,
        autoSyncMode: 2,
        targetSchema: "",
        batchSize: 5000,
        channelSize: 100,
        targetSyncOption: 'INSERT_UPDATE_DELETE',
        beforeSqlScripts: '',
        afterSqlScripts: '',
      },
      rules: {
        name: [
          {
            required: true,
            message: "任务名称不能为空",
            trigger: "blur"
          }
        ],
        scheduleMode: [
          {
            required: true,
            type: 'string',
            message: "调度方式必须选择",
            trigger: "change"
          }
        ],
        sourceConnectionId: [
          {
            required: true,
            type: 'integer',
            message: "必选选择一个来源端",
            trigger: "change"
          }
        ],
        sourceSchema: [
          {
            required: true,
            type: 'string',
            message: "必选选择一个Schema名",
            trigger: "change"
          }
        ],
        tableType: [
          {
            required: true,
            type: 'string',
            message: "表类型必须选择",
            trigger: "change"
          }
        ],
        includeOrExclude: [
          {
            required: true,
            type: 'string',
            message: "配置方式必须选择",
            trigger: "change"
          }
        ],
        sourceTables: [
          {
            required: false,
            type: 'array',
            message: "必选选择一个Table名",
            trigger: "change"
          }
        ],
        targetConnectionId: [
          {
            required: true,
            type: 'integer',
            message: "必选选择一个目的端",
            trigger: "change"
          }
        ],
        targetSchema: [
          {
            required: true,
            type: 'string',
            message: "必选选择一个Schema名",
            trigger: "change"
          }
        ],
        batchSize: [
          {
            required: true,
            type: 'integer',
            message: "必选选择一个数据批次大小",
            trigger: "change"
          }
        ],
        channelSize: [
          {
            required: true,
            type: 'integer',
            message: "必选选择一个通道队列大小",
            trigger: "change"
          }
        ],
        targetSyncOption: [
          {
            required: true,
            type: 'string',
            message: "必选选择一个同步方法来 ",
            trigger: "change"
          }
        ]
      },
      can_go_back: true,
      active: 5,
      sourceConnection: {},
      targetConnection: {},
      sourceConnectionSchemas: [],
      sourceSchemaTables: [],
      targetConnectionSchemas: [],
      tableNameMapperDialogVisible: false,
      columnNameMapperDialogVisible: false,
      tableNamesMapperData: [],
      columnNamesMapperData: [],
      preiveSeeTableNameList: [],
      preiveTableName: "",
    }
  },
  methods: {
    handleClose (done) {
    },
    next () {
      this.can_go_back = false;
      if (this.active++ > 4) this.active = 5
    },
    pre () {
      this.can_go_back = false;
      if (this.active-- < 2) this.active = 1
    },
    handleGoBack () {
      if (this.can_go_back) {
        this.$router.go(-1);
      }
    },
    loadConnections: function () {
      this.connectionNameList = [];
      this.$http({
        method: "GET",
        url: "/dbswitch/admin/api/v1/connection/list/name"
      }).then(
        res => {
          if (0 === res.data.code) {
            this.connectionNameList = res.data.data;
          } else {
            this.connectionNameList = [];
            if (res.data.message) {
              alert("加载任务列表失败:" + res.data.message);
            }
          }
        },
        function () {
          console.log("failed");
        }
      );
    },
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
            batchSize: detail.configuration.batchSize,
            channelSize: detail.configuration.channelSize,
            targetSyncOption: detail.configuration.targetSyncOption,
            beforeSqlScripts: detail.configuration.beforeSqlScripts,
            afterSqlScripts: detail.configuration.afterSqlScripts,
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
    changeUpdateCronExpression: function (value) {
      this.updateform.cronExpression = value;
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
    selectUpdateChangedTableType: function (value) {
      this.sourceSchemaTables = [];
      if ('TABLE' === value) {
        this.$http.get(
          "/dbswitch/admin/api/v1/connection/tables/get/" + this.updateform.sourceConnectionId + "?schema=" + this.updateform.sourceSchema
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
          "/dbswitch/admin/api/v1/connection/views/get/" + this.updateform.sourceConnectionId + "?schema=" + this.updateform.sourceSchema
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
    addTableNameMapperListRow: function () {
      if (!this.updateform.tableNameMapper) {
        this.updateform.tableNameMapper = [];
      }
      this.updateform.tableNameMapper.push({ "fromPattern": "", "toValue": "" });
    },
    deleteTableNameMapperListItem: function (index) {
      if (!this.updateform.tableNameMapper) {
        this.updateform.tableNameMapper = [];
      }
      this.updateform.tableNameMapper.splice(index, 1);
    },
    previewTableNameMapList: function () {
      if (this.updateform.sourceConnectionId <= 0 || !this.updateform.sourceSchema || this.updateform.sourceSchema.length == 0) {
        alert("请选择源端数据源和源端模式名！");
        return;
      }
      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/mapper/preview/table",
        data: JSON.stringify({
          id: this.updateform.sourceConnectionId,
          schemaName: this.updateform.sourceSchema,
          isInclude: this.updateform.includeOrExclude == 'INCLUDE',
          tableNames: this.updateform.sourceTables,
          nameMapper: this.updateform.tableNameMapper,
          tableNameCase: this.updateform.tableNameCase
        })
      }).then(res => {
        if (0 === res.data.code) {
          this.tableNamesMapperData = res.data.data;
          this.tableNameMapperDialogVisible = true;
        } else {
          if (res.data.message) {
            alert(res.data.message);
          }
        }
      });

    },
    addColumnNameMapperListRow: function () {
      if (!this.updateform.columnNameMapper) {
        this.updateform.columnNameMapper = [];
      }
      this.updateform.columnNameMapper.push({ "fromPattern": "", "toValue": "" });
    },
    deleteColumnNameMapperListItem: function (index) {
      if (!this.updateform.columnNameMapper) {
        this.updateform.columnNameMapper = [];
      }
      this.updateform.columnNameMapper.splice(index, 1);
    },
    previewColumnNameMapList: function () {
      if (!this.updateform.sourceConnectionId || this.updateform.sourceConnectionId <= 0
        || !this.updateform.sourceSchema || this.updateform.sourceSchema.length == 0) {
        alert("请选择【源端数据源】和【源端模式名】！");
        return;
      }

      if (!this.updateform.includeOrExclude) {
        alert("请选择源端表选择的【配置方式】！");
        return;
      }

      if (this.updateform.includeOrExclude == "INCLUDE") {
        if (this.updateform.sourceTables && this.updateform.sourceTables.length > 0) {
          this.preiveSeeTableNameList = this.updateform.sourceTables;
        } else {
          this.preiveSeeTableNameList = this.sourceSchemaTables;
        }
      } else {
        if (this.updateform.sourceTables.length == 0) {
          alert("请选择排除表的【表名配置】！");
          return;
        }

        // 排除表，求差集
        this.preiveSeeTableNameList = JSON.parse(JSON.stringify(this.sourceSchemaTables));
        for (var i = 0; i < this.updateform.sourceTables.length; ++i) {
          var one = this.updateform.sourceTables[i];
          this.preiveSeeTableNameList.some((item, index) => {
            if (item == one) {
              this.preiveSeeTableNameList.splice(index, 1)
              return true;
            }
          })
        }
      }

      this.preiveTableName = "";
      this.columnNamesMapperData = [];
      this.columnNameMapperDialogVisible = true;
    },
    queryPreviewColumnNameMapperList: function () {
      if (!this.preiveSeeTableNameList || this.preiveSeeTableNameList.length == 0) {
        alert("请在源端配置【表名配置】！");
        return;
      }

      if (!this.preiveTableName || this.preiveTableName.length == 0) {
        alert("请选择一个表名！");
        return;
      }

      this.$http({
        method: "POST",
        headers: {
          'Content-Type': 'application/json'
        },
        url: "/dbswitch/admin/api/v1/mapper/preview/column",
        data: JSON.stringify({
          id: this.updateform.sourceConnectionId,
          schemaName: this.updateform.sourceSchema,
          isInclude: this.updateform.includeOrExclude == 'INCLUDE',
          tableName: this.preiveTableName,
          nameMapper: this.updateform.columnNameMapper,
          columnNameCase: this.updateform.columnNameCase
        })
      }).then(res => {
        if (0 === res.data.code) {
          this.columnNamesMapperData = res.data.data;
        } else {
          if (res.data.message) {
            alert(res.data.message);
          }
        }
      });

    },
    handleSave: function () {
      if (0 === this.updateform.autoSyncMode) {
        this.updateform.targetDropTable = false;
        this.updateform.targetOnlyCreate = false;
      } else if (1 === this.updateform.autoSyncMode) {
        this.updateform.targetDropTable = true;
        this.updateform.targetOnlyCreate = true;
      } else {
        this.updateform.targetDropTable = true;
        this.updateform.targetOnlyCreate = false;
      }

      this.$refs['updateform'].validate(valid => {
        if (valid) {
          this.$http({
            method: "POST",
            headers: {
              'Content-Type': 'application/json'
            },
            url: "/dbswitch/admin/api/v1/assignment/update",
            data: JSON.stringify({
              id: this.$route.query.id,
              name: this.updateform.name,
              description: this.updateform.description,
              scheduleMode: this.updateform.scheduleMode,
              cronExpression: this.updateform.cronExpression,
              config: {
                sourceConnectionId: this.updateform.sourceConnectionId,
                sourceSchema: this.updateform.sourceSchema,
                tableType: this.updateform.tableType,
                includeOrExclude: this.updateform.includeOrExclude,
                sourceTables: this.updateform.sourceTables,
                targetConnectionId: this.updateform.targetConnectionId,
                targetSchema: this.updateform.targetSchema,
                tableNameMapper: this.updateform.tableNameMapper,
                columnNameMapper: this.updateform.columnNameMapper,
                tableNameCase: this.updateform.tableNameCase,
                columnNameCase: this.updateform.columnNameCase,
                targetDropTable: this.updateform.targetDropTable,
                targetOnlyCreate: this.updateform.targetOnlyCreate,
                targetAutoIncrement: this.updateform.targetAutoIncrement,
                batchSize: this.updateform.batchSize,
                channelSize: this.updateform.channelSize,
                targetSyncOption: this.updateform.targetSyncOption,
                beforeSqlScripts: this.updateform.beforeSqlScripts,
                afterSqlScripts: this.updateform.afterSqlScripts,
              }
            })
          }).then(res => {
            if (0 === res.data.code) {
              this.$message("修改任务成功");
              this.$router.push('/task/assignment')
            } else {
              if (res.data.message) {
                alert(res.data.message);
              }
            }
          });
        } else {
          alert("请点击【上一步】检查输入");
        }
      });
    }
  },
  created () {
    this.loadConnections();
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

.tip-content {
  font-size: 12px;
}

.name-mapper-table,
.name-mapper-table table tr th,
.name-mapper-table table tr td {
  border-collapse: collapse;
  border: 1px solid #e0dddd;
  width: 100%;
}

.el-descriptions__body
  .el-descriptions__table
  .el-descriptions-row
  .el-descriptions-item__label {
  min-width: 20px;
  max-width: 60px;
}
</style>