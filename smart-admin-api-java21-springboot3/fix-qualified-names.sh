#!/bin/bash

# Fix DepartmentCacheManager
sed -i 's/net\.lab1024\.sa\.common\.cache\.CacheService/net.lab1024.sa.foundation.cache.CacheService/g' \
  sa-admin/src/main/java/net/lab1024/sa/admin/module/system/department/manager/DepartmentCacheManager.java

# Fix JetCacheAutoConfiguration
sed -i 's/"net\.lab1024\.sa\.common\.cache"/"net.lab1024.sa.foundation.cache"/g' \
  sa-base/foundation/cache/src/main/java/net/lab1024/sa/foundation/cache/config/JetCacheAutoConfiguration.java

# Fix test file package declarations
sed -i 's/package net\.lab1024\.sa\.common\.mq\.kafka/package net.lab1024.sa.foundation.mq.kafka/g' \
  sa-base/foundation/mq/src/test/java/net/lab1024/sa/foundation/mq/kafka/batch/MessageAggregatorTest.java \
  sa-base/foundation/mq/src/test/java/net/lab1024/sa/foundation/mq/kafka/core/KafkaProducerServiceTest.java \
  sa-base/foundation/mq/src/test/java/net/lab1024/sa/foundation/mq/kafka/listener/AbstractBatchKafkaListenerTest.java

# Fix Lock4jAutoConfiguration
sed -i 's/"net\.lab1024\.sa\.common\.redislock"/"net.lab1024.sa.foundation.redislock"/g' \
  sa-base/foundation/redis-lock/src/main/java/net/lab1024/sa/foundation/redislock/config/Lock4jAutoConfiguration.java

# Fix RepeatSubmitAspect
sed -i 's/net\.lab1024\.sa\.common\.repeatsubmit\.annotation/net.lab1024.sa.foundation.repeatsubmit.annotation/g' \
  sa-base/foundation/repeat-submit/src/main/java/net/lab1024/sa/foundation/repeatsubmit/aspect/RepeatSubmitAspect.java

echo "✓ Fixed all fully-qualified package names"
