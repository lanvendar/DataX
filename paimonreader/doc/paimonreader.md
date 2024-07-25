# DataX PaimonReader 插件文档


------------

## 1 快速介绍

PaimonReader提供读取Paimon表的功能。


## 2 功能与限制

* (1)、目前PaimonReader仅支持简单条件查询
* (2)、PaimonReader仅支持表中的字段，不支持复杂表达式
* (3)、PaimonReader仅支持where条件的and组合不支持or条件
* (4)、PaimonReader不支持排序以及分组
* (5)、其他请参考[Paimon](https://paimon.apache.org/)

## 3 功能说明


### 3.1 配置样例

```json
{
	"job": {
		"content": [
			{
				"reader": {
					"name": "paimonreader",
					"parameter": {
						"warehouse": "hdfs:///paimon/data",
						"metastore": "hive",
						"uri": "thrift://bd02:9083",
						"hiveConfDir": null,
						"hadoopConfDir": null,
						"database": "bigdata_proj",
						"table": "test_person2",
						"sdl": "select * from test_person2 where name='zhangsan'"
					}
				},
				"writer": {
					"name": "paimonwriter",
					"parameter": {
						"warehouse": "hdfs:///paimon/test-data",
						"metastore": "hive",
						"uri": "thrift://bd02:9083",
						"tableType": "external",
						"hiveConfDir": null,
						"hadoopConfDir": null,
						"database": "nimbus_bigdata_lzk_2",
						"table": "peopletest",
						"primaryKey": "name",
						"partitionKey": null,
						"mode": "upsert",
						"column": [
							{
								"name": "name",
								"type": "string"
							},
							{
								"name": "age",
								"type": "int"
							}
						],
						"paimonConf": {
							"full-compaction.delta-commits": "1",
							"file.format": "parquet",
							"bucket": "1",
							"snapshot.num-retained.min": "5"
						}
					}
				}
			}
		],
		"setting": {
			"errorLimit": {
				"record": 0,
				"percentage": 0.02
			},
			"speed": {
				"byte": 10485760
			}
		}
	}
}
```

### 3.2 参数说明

* **warehouse**

	* 描述：Paimon warehouse的存储路径。格式：hdfs://ip:端口；例如：hdfs://127.0.0.1:9000<br />

	* 必选：是 <br />

	* 默认值：无 <br />

* **metastore**

	* 描述：metastore类型，目前只支持用户配置为"hive"。 <br />

	* 必选：是 <br />

	* 默认值：无 <br />
* **uri**

	* 描述：hive metastore的thrift地址。格式：thrift://ip:端口；例如：thrift://127.0.0.1:9083<br />

	* 必选：是 <br />

	* 默认值：无 <br />

* **database**

 	* 描述：database名称。 <br />

	* 必选：是 <br />

	* 默认值：无 <br />

* **table**

	* 描述：table名称。 <br />

	* 必选：是 <br />

	* 默认值：无 <br />

* **sql**

	* 描述：查询SQL。 <br />

	* 必选：否 <br />

	* 默认值：无 <br />

* **hiveConfDir**

	* 描述：hive配置文件目录。 <br />

	* 必选：否 <br />

	* 默认值：无 <br />

* **hadoopConfDir**

	* 描述：hadoop配置文件目录。<br />

	* 必选：否 <br />

 	* 默认值：无 <br />


### 3.3 类型转换

目前 PaimonReader 支持大部分 Paimon 类型，请注意检查你的类型。

下面列出 PaimonReader 针对 Paimon 数据类型转换列表:

| DataX 内部类型| PAIMON 数据类型                 |
| -------- |-----------------------------|
| Long     | TINYINT,SMALLINT,INT,BIGINT |
| Double   | FLOAT,DOUBLE                |
| String   | STRING,VARCHAR,CHAR         |
| Boolean  | BOOLEAN                     |
| Date     | DATE,TIMESTAMP              |


## 4 配置步骤
略

## 5 约束限制

略

## 6 FAQ

略
