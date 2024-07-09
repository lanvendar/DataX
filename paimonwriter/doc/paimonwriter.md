# DataX PaimonWriter 插件文档


------------

## 1 快速介绍

PaimonWriter提供向hive中写入Paimon表。


## 2 功能与限制

* (1)、目前PaimonWriter仅支持写入非分区表并且bucket固定为1
* (2)、Paimon的Catalog类型固定为hive
* (3)、如果database或者table不存在，支持自动创建
* (4)、如果database已存在则无法改变外部表或者内部表
* (5)、如果table已存在则无法改变表属性
* (6)、其他请参考[Paimon](https://paimon.apache.org/)

## 3 功能说明


### 3.1 配置样例

```json
{
	"job": {
		"content": [
			{
				"reader": {
					"name": "postgresqlreader",
					"parameter": {
						"column": [
							"name",
							"age"
						],
						"password": "postgres",
						"connection": [
							{
								"jdbcUrl": [
									"jdbc:postgresql://172.21.234.20:5432/nimbus-bdmm-lzk?characterEncoding=UTF-8&autoReconnect=true&useSSL=false&serverTimezone=Asia/Shanghai"
								],
								"table": [
									"people"
								]
							}
						],
						"where": "",
						"username": "postgres"
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

* **column**

	* 描述：写入数据的字段，不支持对部分列写入。为与hive中表关联，需要指定表中所有字段名和字段类型，其中：name指定字段名，type指定字段类型。 <br />

		用户可以指定Column字段信息，配置如下：

		```json
		"column":
                 [
                            {
                                "name": "userName",
                                "type": "string"
                            },
                            {
                                "name": "age",
                                "type": "long"
                            }
                 ]
		```

	* 必选：是 <br />

	* 默认值：无 <br />
* **mode**

 	* 描述：paimonwriter写入模式： <br />

		* upsert，写入与原有数据进行upsert操作。
		* truncate，写入前先清表然后写入。

	* 必选：是 <br />

	* 默认值：无 <br />

* **tableType**

	* 描述：外部表还是内部表 <br />

		* external，外部表。
		* managed，内部表。
      
	* 必选：否 <br />

	* 默认值：managed <br />

* **hiveConfDir**

	* 描述：hive配置文件目录。 <br />

	* 必选：否 <br />

	* 默认值：无 <br />

* **hadoopConfDir**

	* 描述：hadoop配置文件目录。<br />

	* 必选：否 <br />

 	* 默认值：无 <br />

* **paimonConf**

* 描述：paimon表的其他可选配置。<br />

  ```json
  "paimonConf" : {
		"full-compaction.delta-commits": "1",
		"file.format": "parquet",
		"bucket": "1",
		"snapshot.num-retained.min": "5"
  }

  ```

  * 必选：否 <br />

  * 默认值：无 <br />


### 3.3 类型转换

目前 PaimonWriter 支持大部分 Paimon 类型，请注意检查你的类型。

下面列出 PaimonWriter 针对 Paimon 数据类型转换列表:

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
