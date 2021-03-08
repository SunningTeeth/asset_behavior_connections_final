package com.lanysec.services;

import com.alibaba.fastjson.JSON;
import com.lanysec.check.CheckFlowEntity;
import com.lanysec.check.FlowMatchCheckEntity;
import com.lanysec.check.ModelCheckParamsEntity;
import com.lanysec.check.ModelCheckParamsRichSourceFunction;
import com.lanysec.config.JavaKafkaConfigurer;
import com.lanysec.config.ModelParamsConfigurer;
import com.lanysec.entity.AssetSourceEntity;
import com.lanysec.entity.FlowDstMatchAssetEntity;
import com.lanysec.entity.FlowEntity;
import com.lanysec.entity.FlowSrcMatchAssetEntity;
import com.lanysec.utils.ConversionUtil;
import com.lanysec.utils.DbConnectUtil;
import com.lanysec.utils.StringUtil;
import com.lanysec.utils.SystemUtil;
import com.mysql.jdbc.authentication.MysqlClearPasswordPlugin;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.serialization.TypeInformationSerializationSchema;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.io.jdbc.JDBCInputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;
import org.apache.flink.streaming.connectors.kafka.internals.KeyedSerializationSchemaWrapper;
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Filter;

/**
 * @author daijb
 * @date 2021/3/5 21:36
 */
public class KafkaMessageStreaming implements AssetBehaviorConstants {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageStreaming.class);

    public static void main(String[] args) {
        KafkaMessageStreaming kafkaMessageStreaming = new KafkaMessageStreaming();
        // 启动任务
        kafkaMessageStreaming.run(args);
    }

    public void run(String[] args) {
        logger.info("flink streaming is starting....");
        StreamExecutionEnvironment streamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        // 重试4次，每次间隔20s
        streamExecutionEnvironment.setRestartStrategy(RestartStrategies.fixedDelayRestart(4, Time.of(20, TimeUnit.SECONDS)));
        streamExecutionEnvironment.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        EnvironmentSettings fsSettings = EnvironmentSettings.newInstance().useOldPlanner().inStreamingMode().build();
        //创建 TableEnvironment
        StreamTableEnvironment streamTableEnvironment = StreamTableEnvironment.create(streamExecutionEnvironment, fsSettings);

        //每隔60s进行启动一个检查点【设置checkpoint的周期】
        //streamExecutionEnvironment.enableCheckpointing(1000 * 60);
        //设置模式为：exactly_one，仅一次语义
        //streamExecutionEnvironment.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        //确保检查点之间有5s的时间间隔【checkpoint最小间隔】
        //streamExecutionEnvironment.getCheckpointConfig().setMinPauseBetweenCheckpoints(1000 * 5);
        //检查点必须在10s之内完成，或者被丢弃【checkpoint超时时间】
        //streamExecutionEnvironment.getCheckpointConfig().setCheckpointTimeout(10000);
        //同一时间只允许进行一次检查点
        //streamExecutionEnvironment.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        //表示一旦Flink程序被cancel后，会保留checkpoint数据，以便根据实际需要恢复到指定的checkpoint
        //streamExecutionEnvironment.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        //设置statebackend,将检查点保存在hdfs上面，默认保存在内存中。这里先保存到本地
        //streamExecutionEnvironment.setStateBackend(new FsStateBackend("file:///Users/temp/cp/"));

        //加载kafka配置信息
        Properties kafkaProperties = JavaKafkaConfigurer.getKafkaProperties(args);
        logger.info("load kafka properties : " + kafkaProperties.toString());
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getProperty("bootstrap.servers"));
        //可根据实际拉取数据等设置此值，默认30s
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        //每次poll的最大数量
        //注意该值不要改得太大，如果poll太多数据，而不能在下次poll之前消费完，则会触发一次负载均衡，产生卡顿
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 30);
        //当前消费实例所属的消费组
        //属于同一个组的消费实例，会负载消费消息
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getProperty("group.id"));

        // 添加kafka source
        DataStream<String> kafkaSource = streamExecutionEnvironment.addSource(new FlinkKafkaConsumer010<>(kafkaProperties.getProperty("topic"), new SimpleStringSchema(), props));

        /**
         * 过滤kafka无匹配资产的数据
         */
        SingleOutputStreamOperator<String> kafkaSourceFilter = kafkaSource.filter((FilterFunction<String>) value -> {
            JSONObject line = (JSONObject) JSONValue.parse(value);
            if (!StringUtil.isEmpty(ConversionUtil.toString(line.get("SrcID")))) {
                return true;
            }
            if (!StringUtil.isEmpty(ConversionUtil.toString(line.get("DstID")))) {
                return true;
            }
            return false;
        });

        // 启动定时任务
        startFunc();
        //建模系列操作_new
        {
            SingleOutputStreamOperator<String> map = kafkaSourceFilter.map(new AssetMapSourceFunction());
            map.print().setParallelism(1);
            map.addSink(new MysqlSink2());
        }

        //建模系列操作_old
        {
            //建模entity
            /*DataStream<FlowEntity> kafkaProcessStream = kafkaSource.process(new ParserKafkaProcessFunction());

            // 更新状态
            updateModelTaskStatus(ModelStatus.RUNNING);

            //kafka数据过滤含有资产id
            DataStream<FlowEntity> kafkaFilterSourceStream = kafkaProcessStream.filter((FilterFunction<FlowEntity>) flowEntity -> {
                // 匹配含有资产的
                if (flowEntity == null) {
                    return false;
                }
                if (!StringUtil.isEmpty(flowEntity.getDstId()) || !StringUtil.isEmpty(flowEntity.getSrcId())) {
                    return true;
                }
                return true;
            });

            // 添加mysql.asset source
            DataStream<AssetSourceEntity> assetSourceProcessStream = streamExecutionEnvironment.addSource(new AssetRichSourceFunction()).process(new ParserAssetProcessFunction());

            // 注册kafka关联表
            streamTableEnvironment.createTemporaryView("kafka_source", kafkaFilterSourceStream, "srcId,srcIp,dstId,dstIp,areaId,flowId,rTime,rowtime.rowtime");

            // 注册asset关联表
            streamTableEnvironment.createTemporaryView("asset_source", assetSourceProcessStream, "entityId,entityName,assetIp,areaId");

            // 运行sql
            String temporarySrcIdSql = "select entityId,assetIp,dstId,dstIp " +
                    " from kafka_source ks,asset_source a " +
                    " where ks.srcId = a.entityId ";

            String temporaryDstIdSql = "select entityId,assetIp,srcId,srcIp " +
                    " from kafka_source ks,asset_source a " +
                    " where ks.dstId = a.entityId ";

            // 获取结果
            Table kafkaSrcIdTable = streamTableEnvironment.sqlQuery(temporarySrcIdSql);
            Table kafkaDstIdTable = streamTableEnvironment.sqlQuery(temporaryDstIdSql);

            DataStream<Tuple2<Boolean, FlowSrcMatchAssetEntity>> tuple2SrcDataStream = streamTableEnvironment.toRetractStream(kafkaSrcIdTable, FlowSrcMatchAssetEntity.class);
            DataStream<Tuple2<Boolean, FlowDstMatchAssetEntity>> tuple2DstDataStream = streamTableEnvironment.toRetractStream(kafkaDstIdTable, FlowDstMatchAssetEntity.class);

            //转换数据格式
            DataStream<JSONObject> kafkaFlowSrcEntityJson = tuple2SrcDataStream.map((MapFunction<Tuple2<Boolean, FlowSrcMatchAssetEntity>, JSONObject>) value -> value.f1.toJSONObject());
            DataStream<JSONObject> kafkaFlowDstEntityJson = tuple2DstDataStream.map((MapFunction<Tuple2<Boolean, FlowDstMatchAssetEntity>, JSONObject>) value -> value.f1.toJSONObject());

            kafkaFlowSrcEntityJson.addSink(new MySqlSink());
            kafkaFlowDstEntityJson.addSink(new MySqlSink());*/

            //DataStream<FlowSrcMatchAssetEntity> flowEntityDataSrcStream = streamTableEnvironment.toAppendStream(kafkaSrcIdTable, FlowSrcMatchAssetEntity.class);
            //DataStream<FlowDstMatchAssetEntity> flowEntityDataDstStream = streamTableEnvironment.toAppendStream(kafkaDstIdTable, FlowDstMatchAssetEntity.class);

            /**
             * 转换数据格式
             */
           /* DataStream<JSONObject> kafkaFlowSrcEntityJson = flowEntityDataSrcStream.map(new MapFunction<FlowSrcMatchAssetEntity, JSONObject>() {
                @Override
                public JSONObject map(FlowSrcMatchAssetEntity flowSrcMatchAssetEntity) throws Exception {
                    return flowSrcMatchAssetEntity.toJSONObject();
                }
            });

            DataStream<JSONObject> kafkaFlowDstEntityJson = flowEntityDataDstStream.map(
                    (MapFunction<FlowDstMatchAssetEntity, JSONObject>) FlowDstMatchAssetEntity::toJSONObject);

            kafkaFlowSrcEntityJson.addSink(new MySqlSink());
            kafkaFlowDstEntityJson.addSink(new MySqlSink());*/


        }

        // 检测系列操作_new
        {
            /*SingleOutputStreamOperator<String> checkStreamMap = kafkaSourceFilter.map(new CheckModelMapSourceFunction());
            SingleOutputStreamOperator<String> matchCheckStreamFilter = checkStreamMap.filter((FilterFunction<String>) value -> {
                if (StringUtil.isEmpty(value)) {
                    return false;
                }
                return true;
            });*/

            // matchCheckStreamFilter.print().setParallelism(1);
            // 将过滤数据送往kafka csp_event topic
            String brokers = ConversionUtil.toString(props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));

            /*matchCheckStreamFilter.addSink(new FlinkKafkaProducer010<>(
                    brokers,
                    "csp_event",
                    new KeyedSerializationSchemaWrapper<>(new SimpleStringSchema())
            ));*/
        }

        // 检测系列操作_old
        {
            //检测entity
            //DataStream<CheckFlowEntity> checkProcessStream = kafkaSource.process(new ParserKafkaProcessFunction0());

            //kafka数据过滤含有资产id
            /*DataStream<CheckFlowEntity> detectionKafkaFilterSourceStream = checkProcessStream.filter((FilterFunction<CheckFlowEntity>) flowEntity -> {
                // 匹配含有资产的
                if (flowEntity == null || StringUtil.isEmpty(flowEntity.getDstId()) || StringUtil.isEmpty(flowEntity.getSrcId())) {
                    return false;
                }
                return true;
            });*/

            // 添加mysql.asset source
            //DataStream<ModelCheckParamsEntity> modelCheckParamsEntityDataStream = streamExecutionEnvironment.addSource(new ModelCheckParamsRichSourceFunction()).process(new ParserModelCheckParamsProcessFunction());

            // 注册kafka关联表
            //streamTableEnvironment.createTemporaryView("kafka_source_check", detectionKafkaFilterSourceStream, "srcId,srcIp,srcPort,dstId,dstIp,dstPort,id,l4p,l7p");

            // 注册model_check_params关联表
            //streamTableEnvironment.createTemporaryView("model_check_params_source", modelCheckParamsEntityDataStream, "modelingParamsId,srcIp,srcId,dstIpSegment,modelCheckAltParams");

            // 运行sql
           /* String temporarySrcSql = "select " +
                    "ks.srcId,ks.srcIp,ks.srcPort,ks.dstId,ks.dstIp,ks.dstPort,ks.id,ks.l4p,ks.l7p,cs.dstIpSegment,cs.modelCheckAltParams" +
                    " from kafka_source_check ks,model_check_params_source cs " +
                    " where ks.srcId = cs.srcId ";

            String temporaryDstSql = "select " +
                    "ks.srcId,ks.srcIp,ks.srcPort,ks.dstId,ks.dstIp,ks.dstPort,ks.id,ks.l4p,ks.l7p,cs.dstIpSegment,cs.modelCheckAltParams" +
                    " from kafka_source_check ks,model_check_params_source cs " +
                    " where ks.dstId = cs.srcId ";*/

            // 获取结果
            //Table kafkaSrcIdTable = streamTableEnvironment.sqlQuery(temporarySrcSql);
            //Table kafkaDstIdTable = streamTableEnvironment.sqlQuery(temporaryDstSql);

            //DataStream<FlowMatchCheckEntity> flowEntityDataSrcStream = streamTableEnvironment.toAppendStream(kafkaSrcIdTable, FlowMatchCheckEntity.class);
            //DataStream<FlowMatchCheckEntity> flowEntityDataDstStream = streamTableEnvironment.toAppendStream(kafkaDstIdTable, FlowMatchCheckEntity.class);

            /**
             * flow src 过滤
             */
            /*DataStream<FlowMatchCheckEntity> matchCheckEntityDataSrcFilterStream = flowEntityDataSrcStream.filter((FilterFunction<FlowMatchCheckEntity>) entity -> {

                //TODO 排除白名单
                String srcIp = entity.getSrcIp();
                JSONObject obj = (JSONObject) JSONValue.parse(entity.getModelCheckAltParams());
                Object whiteIps = obj.get("white_ip_list");
                if (whiteIps != null) {
                    JSONArray whiteIpsList = (JSONArray) JSONValue.parse(whiteIps.toString());
                    if (whiteIpsList != null && !whiteIpsList.isEmpty()) {
                        Set whileIpSet = new HashSet(whiteIpsList);
                        if (whileIpSet.contains(srcIp)) {
                            return false;
                        }
                    }
                }
                JSONArray dstIpSegment = (JSONArray) JSONValue.parse(entity.getDstIpSegment());
                // 分时统计的,如何检测？？？
                if (false) {
                    //TODO 检测对应资产连接的目的IP是否在建模的网段
                    return false;
                }
                return true;
            });*/

            /**
             * flow dst 过滤
             */
           /* DataStream<FlowMatchCheckEntity> matchCheckEntityDataDstFilterStream = flowEntityDataDstStream.filter((FilterFunction<FlowMatchCheckEntity>) entity -> {
                String dstIp = entity.getDstIp();
                //TODO 排除白名单
                JSONObject obj = (JSONObject) JSONValue.parse(entity.getModelCheckAltParams());
                Object whiteIps = obj.get("white_ip_list");
                if (whiteIps != null) {
                    JSONArray whiteIpsList = (JSONArray) JSONValue.parse(whiteIps.toString());
                    if (whiteIpsList != null && !whiteIpsList.isEmpty()) {
                        Set whileIpSet = new HashSet(whiteIpsList);
                        if (whileIpSet.contains(dstIp)) {
                            return false;
                        }
                    }
                }
                JSONArray dstIpSegment = (JSONArray) JSONValue.parse(entity.getDstIpSegment());
                // 分时统计的,如何检测？？？
                if (true) {
                    //TODO 检测对应资产连接的目的IP是否在建模的网段
                    return false;
                }
                return true;
            });*/

            /**
             * 转换数据格式
             */
            /*DataStream<String> matchCheckEntityDataSrcJson = matchCheckEntityDataSrcFilterStream.map(new MapFunction<FlowMatchCheckEntity, String>() {
                @Override
                public String map(FlowMatchCheckEntity value) throws Exception {
                    return value.toJSONObject().toJSONString();
                }
            });

            DataStream<String> matchCheckEntityDataDstJson = matchCheckEntityDataDstFilterStream.map(new MapFunction<FlowMatchCheckEntity, String>() {
                @Override
                public String map(FlowMatchCheckEntity value) throws Exception {
                    return value.toJSONObject().toJSONString();
                }
            });*/

            // 将过滤数据送往kafka csp_event topic
           /* String brokers = ConversionUtil.toString(props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));

            matchCheckEntityDataSrcJson.addSink(new FlinkKafkaProducer010<>(
                    brokers,
                    "csp_event",
                    new KeyedSerializationSchemaWrapper<>(new SimpleStringSchema())
            ));

            matchCheckEntityDataDstJson.addSink(new FlinkKafkaProducer010<>(
                    brokers,
                    "csp_event",
                    new SimpleStringSchema()
            ));*/

        }

        try {
            streamExecutionEnvironment.execute("kafka message streaming start ....");
        } catch (Exception e) {
            logger.error("flink streaming execute failed", e);
            // 更新状态
            updateModelTaskStatus(ModelStatus.STOP);
        }
    }

    /**
     * 解析asset表数据
     */
    private static class ParserAssetProcessFunction extends ProcessFunction<Tuple4<String, String, String, Integer>, AssetSourceEntity> {

        @Override
        public void processElement(Tuple4<String, String, String, Integer> value, Context ctx, Collector<AssetSourceEntity> out) throws Exception {
            AssetSourceEntity assetSourceEntity = new AssetSourceEntity();
            assetSourceEntity.setEntityId(value.f0);
            assetSourceEntity.setEntityName(value.f1);
            assetSourceEntity.setAssetIp(value.f2);
            assetSourceEntity.setAreaId(value.f3);
            out.collect(assetSourceEntity);
        }
    }

    /**
     * 解析 model_check_params数据
     */
    private static class ParserModelCheckParamsProcessFunction extends ProcessFunction<Tuple5<String, String, String, String, String>, ModelCheckParamsEntity> {

        @Override
        public void processElement(Tuple5<String, String, String, String, String> value, Context ctx, Collector<ModelCheckParamsEntity> out) throws Exception {
            ModelCheckParamsEntity assetSourceEntity = new ModelCheckParamsEntity();
            assetSourceEntity.setModelingParamsId(value.f0);
            assetSourceEntity.setDstIpSegment(value.f1);
            assetSourceEntity.setSrcIp(value.f2);
            assetSourceEntity.setSrcId(value.f3);
            assetSourceEntity.setModelCheckAltParams(value.f4);
            out.collect(assetSourceEntity);
        }
    }

    /**
     * 解析kafka flow 建模数据
     */
    private static class ParserKafkaProcessFunction extends ProcessFunction<String, FlowEntity> {

        @Override
        public void processElement(String value, Context ctx, Collector<FlowEntity> out) throws Exception {
            FlowEntity flowEntity = JSON.parseObject(value, FlowEntity.class);
            //输出到主流
            out.collect(flowEntity);
        }
    }

    /**
     * 解析kafka flow 检测数据
     */
    private static class ParserKafkaProcessFunction0 extends ProcessFunction<String, CheckFlowEntity> {

        @Override
        public void processElement(String value, Context ctx, Collector<CheckFlowEntity> out) throws Exception {
            CheckFlowEntity checkFlowEntity = JSON.parseObject(value, CheckFlowEntity.class);
            //输出到主流
            out.collect(checkFlowEntity);
        }
    }

    /**
     * 更新建模状态
     *
     * @param modelStatus 状态枚举
     */
    private void updateModelTaskStatus(ModelStatus modelStatus) {
        Object modelId = ModelParamsConfigurer.getModelingParams().get(MODEL_ID);
        String updateSql = "UPDATE `modeling_params` SET `model_task_status`=?, `modify_time`=? " +
                " WHERE (`id`='" + modelId + "');";
        DbConnectUtil.execUpdateTask(updateSql, modelStatus.toString().toLowerCase(), LocalDateTime.now().toString());
        logger.info("[kafkaMessageStreaming] update model task status : " + modelStatus.name());
    }


    private void startFunc() {
        logger.info("starting build model params.....");
        new Timer("timer-model").schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    ModelParamsConfigurer.reloadModelingParams();
                    logger.info("reload model params configurer.");
                } catch (Throwable throwable) {
                    logger.error("timer schedule at fixed rate failed ", throwable);
                }
            }
        }, 1000 * 10, 1000 * 60 * 5);

        new Timer("timer-model").schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    ModelParamsConfigurer.queryLastBuildModelResult();
                    logger.info("reload build model result.");
                } catch (Throwable throwable) {
                    logger.error("timer schedule at fixed rate failed ", throwable);
                }
            }
        }, 1000 * 60 * 5, 1000 * 60 * 30);
    }

}
