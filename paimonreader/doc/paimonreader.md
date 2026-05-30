# DataX PaimonReader 插件文档

## 1 快速介绍

PaimonReader 提供从 S3/Ceph 上 Apache Paimon 表批量读取数据的能力，支持字段投影和简单条件下推。

## 2 功能与限制

* `connectType` 当前固定为 `S3`。
* `options` 中列出的固定字段会按语义校验，未知字段会作为 Paimon/Hadoop 配置透传。
* `sql` 支持 `select *` 或选择具体表字段。
* `where` 支持 `and` 组合的简单谓词，包括 `=`, `!=`, `<>`, `>`, `>=`, `<`, `<=`, `is null`, `is not null`, `in`, `not in`, `between`。
* 不支持 `or`、聚合、排序、分组、表达式列、函数列和 join。
* 复杂类型会输出为 JSON 字符串。

## 3 配置样例

```json
{
  "name": "paimonreader",
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
      "sslEnabled": false
    },
    "table": "test_person",
    "sql": "select name, age, tags from test_person where dt = '20260530' and age >= 18"
  }
}
```

`sql` 为空时读取整表全部字段。

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
| `sql` | 查询 SQL。为空时读取整表全部字段。 | 可选 | 无 |

## 5 可扩展参数规则

`options` 的固定字段包括：`warehouse`、`catalogType`、`database`、`endpoint`、`region`、`accessKey`、`accessKeyRef`、`secretKey`、`secretKeyRef`、`pathStyleAccess`、`sslEnabled`。

固定字段必须按语义校验。`options` 允许继续携带 Paimon 官方原生参数或 Hadoop/S3A 协议透传参数，未知字段不会被执行器直接拒绝，会按 KV 形式透传到底层客户端。

当固定字段与动态 KV 同名时，以固定字段为准。

S3/Ceph 连接会自动设置常用 S3A 参数，包括 endpoint、region、access/secret key、path-style、ssl、`S3AFileSystem`、`SimpleAWSCredentialsProvider`，并关闭 S3A change detection 以提升 Ceph 兼容性。

## 6 SQL 支持范围

支持示例：

```sql
select * from test_person
select name, age from test_person where name = 'alice'
select name from test_person where dt = '20260530' and age between 18 and 60
select name from test_person where name in ('alice', 'bob')
select name from test_person where deleted is null
```

不支持示例：

```sql
select count(*) from test_person
select upper(name) from test_person
select name from test_person where age > 18 or city = 'shanghai'
select name from test_person order by age
```

## 7 类型转换

| Paimon 数据类型 | DataX 输出类型 |
| --- | --- |
| `BOOLEAN` | `BoolColumn` |
| `TINYINT`, `SMALLINT`, `INT`, `BIGINT` | `LongColumn` |
| `FLOAT`, `DOUBLE` | `DoubleColumn` |
| `DECIMAL` | `StringColumn`，避免精度损失 |
| `CHAR`, `VARCHAR` | `StringColumn` |
| `DATE`, `TIME` | `LongColumn`，输出毫秒值 |
| `TIMESTAMP` | `DateColumn` |
| `BINARY`, `VARBINARY` | `BytesColumn` |
| `ARRAY`, `MAP`, `ROW` | `StringColumn`，JSON 字符串 |

## 8 FAQ

如果使用 `s3a://` 访问对象存储，运行包需要包含 Hadoop S3A 依赖。当前工程已为 Paimon 插件和 `datax-bundle` 补充 `hadoop-aws`。
