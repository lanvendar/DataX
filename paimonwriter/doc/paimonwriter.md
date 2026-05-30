# DataX PaimonWriter 插件文档

## 1 快速介绍

PaimonWriter 提供向 S3/Ceph 上 Apache Paimon 表批量写入数据的能力，支持追加、主键 upsert、分区覆盖写入。

## 2 功能与限制

* `connectType` 当前固定为 `S3`。
* `loadMode` 支持 `APPEND`、`UPSERT`、`OVERWRITE_PARTITION`。
* 表不存在时支持自动创建 database/table。
* `APPEND` 要求 Paimon 表无主键。
* `UPSERT` 要求 Paimon 表存在主键。
* `OVERWRITE_PARTITION` 要求 Paimon 表是分区表，并且必须显式配置覆盖分区。
* 上游 Record 列数必须与 `column` 配置数量一致。
* `column` 支持配置 Paimon 表字段子集，字段名按 Paimon 表字段名映射，顺序按上游 Record 顺序读取。
* 部分列写入必须包含主键字段；分区字段可以由 `column` 提供，也可以在 `OVERWRITE_PARTITION` 模式下由 `overwritePartition.partition` 提供。
* 类型声明按 StarRocks 官方字段类型解析，再映射到 Paimon 类型。
* 复杂类型字段要求上游以 JSON 字符串传入。
* 单行类型转换失败会收集为 DataX 脏数据；Paimon write/commit 失败会导致任务失败。
* `options` 中列出的固定字段会按语义校验，未知字段会作为 Paimon/Hadoop/S3A 配置或 Paimon 建表属性透传。

## 3 配置样例

### 3.1 APPEND

`APPEND` 用于写入无主键表，自动建表时不要配置 `primaryKey`。

```json
{
  "name": "paimonwriter",
  "parameter": {
    "connectType": "S3",
    "options": {
      "warehouse": "s3a://bucket/paimon/warehouse",
      "catalogType": "filesystem",
      "database": "default",
      "endpoint": "http://127.0.0.1:9000",
      "region": "us-east-1",
      "accessKeyRef": "S3_ACCESS_KEY",
      "secretKeyRef": "S3_SECRET_KEY",
      "pathStyleAccess": true,
      "sslEnabled": false,
      "bucket": "1",
      "bucket-key": "name",
      "file.format": "parquet",
      "snapshot.num-retained.min": "5"
    },
    "table": "test_person",
    "loadMode": "APPEND",
    "batchSize": 1000,
    "overwritePartition": {
      "enabled": false,
      "partition": {
      }
    },
    "primaryKey": "",
    "partitionKey": "",
    "column": [
      {
        "name": "name",
        "type": "varchar"
      },
      {
        "name": "age",
        "type": "int"
      },
      {
        "name": "tags",
        "type": "array<varchar>"
      }
    ]
  }
}
```

### 3.2 UPSERT

`UPSERT` 用于写入主键表。自动建表时必须配置 `primaryKey`；写已有表时目标表也必须是主键表。

```json
{
  "loadMode": "UPSERT",
  "primaryKey": "name",
  "options": {
    "bucket": "1",
    "bucket-key": "name",
    "merge-engine": "deduplicate"
  },
  "column": [
    {
      "name": "name",
      "type": "varchar"
    },
    {
      "name": "age",
      "type": "int"
    }
  ]
}
```

### 3.3 OVERWRITE_PARTITION

`OVERWRITE_PARTITION` 用于覆盖指定分区。为了保证分区覆盖原子性，该模式在任务结束时单次 commit，不按 `batchSize` 多次 commit。

```json
{
  "loadMode": "OVERWRITE_PARTITION",
  "partitionKey": "dt",
  "batchSize": 1000,
  "overwritePartition": {
    "enabled": true,
    "partition": {
      "dt": "20260530"
    }
  },
  "column": [
    {
      "name": "name",
      "type": "varchar"
    },
    {
      "name": "age",
      "type": "int"
    }
  ]
}
```

上例中 `dt` 分区字段未出现在 `column` 中，执行器会使用 `overwritePartition.partition.dt` 作为写入行的分区值。

## 4 参数说明

| 字段 | 说明 | 是否必须 | 默认值 |
| --- | --- | --- | --- |
| `connectType` | 固定为 `S3`。 | 必须 | `S3` |
| `options.warehouse` | Paimon warehouse 路径。 | 必须 | 无 |
| `options.catalogType` | Paimon catalog 类型。 | 可选 | `filesystem` |
| `options.database` | Paimon database。 | 必须 | 无 |
| `options.endpoint` | S3/Ceph endpoint。 | 必须 | 无 |
| `options.region` | S3 region。 | 可选 | `us-east-1` |
| `options.accessKey` | 明文 AccessKey，不推荐。 | 可选 | 无 |
| `options.accessKeyRef` | AccessKey 环境变量名，推荐。 | 可选 | 无 |
| `options.secretKey` | 明文 SecretKey，不推荐。 | 可选 | 无 |
| `options.secretKeyRef` | SecretKey 环境变量名，推荐。 | 可选 | 无 |
| `options.pathStyleAccess` | 是否启用 path-style access。Ceph 场景通常需要开启。 | 可选 | `true` |
| `options.sslEnabled` | 是否启用 SSL。 | 可选 | `false` |
| `table` | Paimon table。 | 必须 | 无 |
| `loadMode` | 写入模式：`APPEND`、`UPSERT`、`OVERWRITE_PARTITION`。 | 必须 | `APPEND` |
| `batchSize` | 每批 commit 的记录数。`OVERWRITE_PARTITION` 模式不按该值多次 commit。 | 可选 | `1000` |
| `overwritePartition` | 覆盖分区配置。 | 可选 | `{"enabled":false}` |
| `primaryKey` | 自动建表时使用的主键，多个字段用逗号分隔。 | 可选 | 无 |
| `partitionKey` | 自动建表时使用的分区键，多个字段用逗号分隔。 | 可选 | 无 |
| `column` | 写入字段和 StarRocks 类型，字段顺序必须与上游 Record 一致。 | 必须 | 无 |

## 5 可扩展参数规则

`options` 的固定字段包括：`warehouse`、`catalogType`、`database`、`endpoint`、`region`、`accessKey`、`accessKeyRef`、`secretKey`、`secretKeyRef`、`pathStyleAccess`、`sslEnabled`。

固定字段必须按语义校验。除固定字段外，`options` 允许继续携带 Paimon 官方原生参数、Hadoop/S3A 协议透传参数或 Paimon 建表属性，未知字段不会被执行器直接拒绝，会按 KV 形式透传。

当固定字段与动态 KV 同名时，以固定字段为准。`batchSize`、`overwritePartition`、`loadMode`、`primaryKey`、`partitionKey`、`column` 等 DataX 自定义控制项必须放在 `parameter` 下，不放入 `options`。

自动建表时，`options` 中非连接类、非 Hadoop/S3A 协议类的 KV 会作为 Paimon table options 写入表属性，例如 `bucket`、`bucket-key`、`file.format`、`snapshot.num-retained.min`、`merge-engine`。

S3/Ceph 连接会自动设置常用 S3A 参数，包括 endpoint、region、access/secret key、path-style、ssl、`S3AFileSystem`、`SimpleAWSCredentialsProvider`，并关闭 S3A change detection 以提升 Ceph 兼容性。

## 6 loadMode 说明

| loadMode | 语义 | 表要求 | RowKind | commit 行为 |
| --- | --- | --- | --- | --- |
| `APPEND` | 追加写入。 | 无主键表 | `INSERT` | 按 `batchSize` 分批 commit |
| `UPSERT` | 主键合并写入。 | 主键表 | `UPDATE_AFTER` | 按 `batchSize` 分批 commit |
| `OVERWRITE_PARTITION` | 覆盖指定分区。 | 分区表 | `INSERT` | 任务结束时单次 commit |

不提供 `TRUNCATE`/全表覆盖模式。全表清空风险较高，建议用 `OVERWRITE_PARTITION` 做有边界的覆盖；如确实需要全表重载，应由外部流程显式清表后再写入。

## 7 类型转换

| StarRocks 字段类型 | Paimon 数据类型 |
| --- | --- |
| `BOOLEAN`, `BOOL` | `BOOLEAN` |
| `TINYINT`, `SMALLINT`, `INT`, `INTEGER`, `BIGINT` | `TINYINT`, `SMALLINT`, `INT`, `BIGINT` |
| `LARGEINT` | `DECIMAL(38,0)` |
| `FLOAT`, `DOUBLE` | `FLOAT`, `DOUBLE` |
| `DECIMAL/DECIMALV2/DECIMAL32/DECIMAL64/DECIMAL128(p,s)` | `DECIMAL(p,s)`，省略参数时为 `DECIMAL(10,0)` |
| `DATE`, `DATETIME`, `TIMESTAMP`, `TIME` | `DATE`, `TIMESTAMP`, `TIME` |
| `CHAR`, `VARCHAR`, `STRING`, `JSON` | `CHAR`, `VARCHAR/STRING` |
| `BINARY`, `VARBINARY`, `BYTEA` | `BINARY`, `VARBINARY` |
| `ARRAY<T>` | `ARRAY<T>` |
| `MAP<K,V>` | `MAP<K,V>` |
| `STRUCT<field:type,...>`, `ROW<field:type,...>` | `ROW<field type,...>` |

复杂类型字段要求上游以 JSON 字符串传入：

```text
ARRAY<INT>                    -> [1,2,3]
MAP<VARCHAR,INT>              -> {"a":1,"b":2}
STRUCT<id:INT,name:VARCHAR>   -> {"id":1,"name":"alice"}
ROW<id:INT,name:VARCHAR>      -> [1,"alice"]
```

`BITMAP`、`HLL`、`PERCENTILE` 等 StarRocks 特殊类型暂不支持。

## 8 脏数据

以下情况按单行脏数据收集，并交由 DataX 脏数据阈值控制任务是否失败：

* 上游 Record 列数与 `column` 配置数量不一致。
* 字段值无法转换为目标 Paimon 类型。
* 复杂类型 JSON 格式错误。
* `ROW` 数组长度与目标字段数不一致。

Paimon write、prepareCommit、commit 等写入链路异常属于任务级错误，会直接导致任务失败。
