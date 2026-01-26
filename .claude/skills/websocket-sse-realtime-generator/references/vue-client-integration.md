# Vue Client Integration for Real-time Communication

**Skill:** websocket-sse-realtime-generator
**Component:** Vue 3 / WebSocket Client / SSE Client
**Purpose:** Client-side real-time communication patterns for SmartAdmin Vue frontend

---

## Dependencies

```bash
# Install SockJS and STOMP client
npm install sockjs-client @stomp/stompjs
```

---

## Pattern 1: WebSocket Client (STOMP + SockJS)

### WebSocket Composable

```javascript
// src/composables/useWebSocket.js
import { ref, onMounted, onUnmounted } from 'vue';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import { getToken } from '@/utils/auth';

export function useWebSocket() {
  const stompClient = ref(null);
  const connected = ref(false);
  const reconnectAttempts = ref(0);
  const maxReconnectAttempts = 5;

  /**
   * Connect to WebSocket server
   */
  const connect = () => {
    const socket = new SockJS('/ws');
    stompClient.value = Stomp.over(socket);

    // Disable debug logging in production
    if (import.meta.env.PROD) {
      stompClient.value.debug = () => {};
    }

    // Connect with Sa-Token
    stompClient.value.connect(
      {
        Authorization: getToken(),
      },
      (frame) => {
        console.log('WebSocket connected:', frame);
        connected.value = true;
        reconnectAttempts.value = 0;
      },
      (error) => {
        console.error('WebSocket error:', error);
        connected.value = false;

        // Auto reconnect with exponential backoff
        if (reconnectAttempts.value < maxReconnectAttempts) {
          reconnectAttempts.value++;
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.value), 30000);
          console.log(`Reconnecting in ${delay}ms (attempt ${reconnectAttempts.value})`);
          setTimeout(connect, delay);
        }
      }
    );
  };

  /**
   * Disconnect from WebSocket
   */
  const disconnect = () => {
    if (stompClient.value && stompClient.value.connected) {
      stompClient.value.disconnect(() => {
        console.log('WebSocket disconnected');
        connected.value = false;
      });
    }
  };

  /**
   * Subscribe to topic
   */
  const subscribe = (destination, callback) => {
    if (stompClient.value && connected.value) {
      return stompClient.value.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        callback(data);
      });
    }
    return null;
  };

  /**
   * Send message to server
   */
  const send = (destination, data) => {
    if (stompClient.value && connected.value) {
      stompClient.value.send(destination, {}, JSON.stringify(data));
    } else {
      console.warn('WebSocket not connected, cannot send message');
    }
  };

  onMounted(() => {
    connect();
  });

  onUnmounted(() => {
    disconnect();
  });

  return {
    stompClient,
    connected,
    connect,
    disconnect,
    subscribe,
    send,
  };
}
```

### Notification Component (WebSocket)

```vue
<template>
  <div class="notification-container">
    <a-badge :count="unreadCount">
      <a-button type="text" @click="showNotifications">
        <BellOutlined />
      </a-button>
    </a-badge>

    <a-drawer
      v-model:open="drawerVisible"
      title="Notifications"
      placement="right"
      width="400"
    >
      <a-list :data-source="notifications" item-layout="vertical">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>{{ item.title }}</template>
              <template #description>{{ item.message }}</template>
            </a-list-item-meta>
            <span>{{ formatTime(item.timestamp) }}</span>
          </a-list-item>
        </template>
      </a-list>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import { BellOutlined } from '@ant-design/icons-vue';
import { useWebSocket } from '@/composables/useWebSocket';

const notifications = ref([]);
const drawerVisible = ref(false);
const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.read).length;
});

const { connected, subscribe } = useWebSocket();

// Subscribe to notifications
subscribe('/topic/notifications', (notification) => {
  notifications.value.unshift({
    ...notification,
    read: false,
    timestamp: Date.now(),
  });

  // Show toast
  message.info(notification.title);
});

// Subscribe to user-specific notifications
subscribe('/user/queue/notifications', (notification) => {
  notifications.value.unshift({
    ...notification,
    read: false,
    timestamp: Date.now(),
  });

  message.success({
    content: notification.title,
    duration: 5,
  });
});

const showNotifications = () => {
  drawerVisible.value = true;
  // Mark as read
  notifications.value.forEach(n => n.read = true);
};

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString();
};
</script>

<style scoped>
.notification-container {
  display: inline-block;
}
</style>
```

---

## Pattern 2: SSE Client

### SSE Composable

```javascript
// src/composables/useSSE.js
import { ref, onMounted, onUnmounted } from 'vue';

export function useSSE(url) {
  const connected = ref(false);
  const reconnectAttempts = ref(0);
  const maxReconnectAttempts = 5;
  let eventSource = null;
  let reconnectTimeout = null;

  /**
   * Connect to SSE endpoint
   */
  const connect = () => {
    eventSource = new EventSource(url);

    eventSource.addEventListener('open', () => {
      console.log('SSE connected');
      connected.value = true;
      reconnectAttempts.value = 0;
    });

    eventSource.addEventListener('error', (error) => {
      console.error('SSE error:', error);
      connected.value = false;

      // Auto reconnect with exponential backoff
      if (reconnectAttempts.value < maxReconnectAttempts) {
        reconnectAttempts.value++;
        const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.value), 30000);
        console.log(`SSE reconnecting in ${delay}ms (attempt ${reconnectAttempts.value})`);
        reconnectTimeout = setTimeout(() => {
          eventSource.close();
          connect();
        }, delay);
      }
    });
  };

  /**
   * Listen to event type
   */
  const addEventListener = (eventType, callback) => {
    if (eventSource) {
      eventSource.addEventListener(eventType, (event) => {
        const data = JSON.parse(event.data);
        callback(data);
      });
    }
  };

  /**
   * Disconnect from SSE
   */
  const disconnect = () => {
    if (eventSource) {
      eventSource.close();
      connected.value = false;
    }
    if (reconnectTimeout) {
      clearTimeout(reconnectTimeout);
    }
  };

  onMounted(() => {
    connect();
  });

  onUnmounted(() => {
    disconnect();
  });

  return {
    connected,
    addEventListener,
    disconnect,
  };
}
```

### Notification Component (SSE)

```vue
<template>
  <div class="notification-container">
    <a-badge :count="unreadCount">
      <a-button type="text" @click="showNotifications">
        <BellOutlined />
      </a-button>
    </a-badge>

    <a-drawer
      v-model:open="drawerVisible"
      title="Notifications"
      placement="right"
      width="400"
    >
      <a-list :data-source="notifications" item-layout="vertical">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>{{ item.title }}</template>
              <template #description>{{ item.message }}</template>
            </a-list-item-meta>
            <span>{{ formatTime(item.timestamp) }}</span>
          </a-list-item>
        </template>
      </a-list>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import { BellOutlined } from '@ant-design/icons-vue';
import { useSSE } from '@/composables/useSSE';

const notifications = ref([]);
const drawerVisible = ref(false);
const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.read).length;
});

const { connected, addEventListener } = useSSE('/api/sse/subscribe');

// Listen to notification events
addEventListener('notification', (notification) => {
  notifications.value.unshift({
    ...notification,
    read: false,
    timestamp: Date.now(),
  });

  message.info(notification.title);
});

const showNotifications = () => {
  drawerVisible.value = true;
  notifications.value.forEach(n => n.read = true);
};

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString();
};
</script>
```

---

## Pattern 3: Real-time Dashboard

### System Metrics Dashboard (WebSocket)

```vue
<template>
  <div class="metrics-dashboard">
    <a-row :gutter="16">
      <a-col :span="12">
        <a-card title="CPU Usage" :loading="!connected">
          <div class="metric-value">{{ metrics.cpuUsage?.toFixed(2) }}%</div>
          <a-progress :percent="metrics.cpuUsage" :status="cpuStatus" />
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card title="Memory Usage" :loading="!connected">
          <div class="metric-value">{{ metrics.memoryUsage?.toFixed(2) }}%</div>
          <a-progress :percent="metrics.memoryUsage" :status="memoryStatus" />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="Metrics History" :loading="!connected">
      <canvas ref="chartCanvas"></canvas>
    </a-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { Chart } from 'chart.js/auto';
import { useWebSocket } from '@/composables/useWebSocket';

const metrics = ref({
  cpuUsage: 0,
  memoryUsage: 0,
  timestamp: 0,
});

const metricsHistory = ref([]);
const chartCanvas = ref(null);
let chart = null;

const { connected, subscribe } = useWebSocket();

// Subscribe to system metrics
subscribe('/topic/system-metrics', (data) => {
  metrics.value = data;

  // Update chart
  metricsHistory.value.push(data);
  if (metricsHistory.value.length > 50) {
    metricsHistory.value.shift();
  }
  updateChart();
});

const cpuStatus = computed(() => {
  if (metrics.value.cpuUsage > 80) return 'exception';
  if (metrics.value.cpuUsage > 60) return 'warning';
  return 'normal';
});

const memoryStatus = computed(() => {
  if (metrics.value.memoryUsage > 80) return 'exception';
  if (metrics.value.memoryUsage > 60) return 'warning';
  return 'normal';
});

onMounted(() => {
  initChart();
});

const initChart = () => {
  chart = new Chart(chartCanvas.value, {
    type: 'line',
    data: {
      labels: [],
      datasets: [
        {
          label: 'CPU Usage (%)',
          data: [],
          borderColor: '#1890ff',
          backgroundColor: 'rgba(24, 144, 255, 0.1)',
        },
        {
          label: 'Memory Usage (%)',
          data: [],
          borderColor: '#52c41a',
          backgroundColor: 'rgba(82, 196, 26, 0.1)',
        },
      ],
    },
    options: {
      responsive: true,
      animation: false,
      scales: {
        y: {
          beginAtZero: true,
          max: 100,
        },
      },
    },
  });
};

const updateChart = () => {
  if (!chart) return;

  chart.data.labels = metricsHistory.value.map((m) =>
    new Date(m.timestamp).toLocaleTimeString()
  );
  chart.data.datasets[0].data = metricsHistory.value.map((m) => m.cpuUsage);
  chart.data.datasets[1].data = metricsHistory.value.map((m) => m.memoryUsage);
  chart.update();
};
</script>

<style scoped>
.metrics-dashboard {
  padding: 20px;
}

.metric-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
}
</style>
```

---

## Pattern 4: Order Status Updates

### Real-time Order List

```vue
<template>
  <div class="order-list">
    <a-table :columns="columns" :data-source="orders" :loading="loading" row-key="orderId">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="getStatusColor(record.status)">
            {{ getStatusText(record.status) }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { useWebSocket } from '@/composables/useWebSocket';
import { getOrderListApi } from '@/api/order';

const orders = ref([]);
const loading = ref(false);

const columns = [
  { title: 'Order ID', dataIndex: 'orderId', key: 'orderId' },
  { title: 'Customer', dataIndex: 'customerName', key: 'customerName' },
  { title: 'Amount', dataIndex: 'amount', key: 'amount' },
  { title: 'Status', dataIndex: 'status', key: 'status' },
  { title: 'Created At', dataIndex: 'createdAt', key: 'createdAt' },
];

const { connected, subscribe } = useWebSocket();

// Fetch initial data
onMounted(async () => {
  loading.value = true;
  try {
    const res = await getOrderListApi();
    orders.value = res.data;
  } catch (error) {
    message.error('Failed to load orders');
  } finally {
    loading.value = false;
  }
});

// Subscribe to order updates
subscribe('/topic/orders', (order) => {
  const index = orders.value.findIndex((o) => o.orderId === order.orderId);

  if (index !== -1) {
    // Update existing order
    orders.value[index] = { ...orders.value[index], ...order };
    message.info(`Order ${order.orderId} status updated to ${order.status}`);
  } else {
    // Add new order
    orders.value.unshift(order);
    message.success(`New order ${order.orderId} received`);
  }
});

// Subscribe to user-specific order updates
subscribe('/user/queue/orders', (order) => {
  const index = orders.value.findIndex((o) => o.orderId === order.orderId);

  if (index !== -1) {
    orders.value[index] = { ...orders.value[index], ...order };
    message.success(`Your order ${order.orderId} status: ${order.status}`);
  }
});

const getStatusColor = (status) => {
  const colors = {
    PENDING: 'default',
    PROCESSING: 'processing',
    COMPLETED: 'success',
    CANCELLED: 'error',
  };
  return colors[status] || 'default';
};

const getStatusText = (status) => {
  const texts = {
    PENDING: 'Pending',
    PROCESSING: 'Processing',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
  };
  return texts[status] || status;
};
</script>
```

---

## Pattern 5: Connection Status Indicator

### Global Connection Status

```vue
<template>
  <div class="connection-status">
    <a-space>
      <a-badge :status="statusType" :text="statusText" />
      <a-button v-if="!connected" type="link" size="small" @click="reconnect">
        Reconnect
      </a-button>
    </a-space>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useWebSocket } from '@/composables/useWebSocket';

const { connected, connect } = useWebSocket();

const statusType = computed(() => {
  return connected.value ? 'success' : 'error';
});

const statusText = computed(() => {
  return connected.value ? 'Connected' : 'Disconnected';
});

const reconnect = () => {
  connect();
};
</script>

<style scoped>
.connection-status {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
}
</style>
```

---

## Best Practices

1. **Composable Pattern:**
   - Create reusable composables for WebSocket and SSE
   - Handle connection, reconnection, and error logic
   - Return clean API for components

2. **Auto Reconnection:**
   - Implement exponential backoff
   - Limit reconnection attempts
   - Show connection status to user

3. **Message Queuing:**
   - Queue messages during disconnection
   - Send queued messages on reconnection
   - Avoid duplicate messages

4. **Memory Management:**
   - Unsubscribe on component unmount
   - Close connections when not needed
   - Limit message history size

5. **User Experience:**
   - Show loading states
   - Display connection status
   - Provide manual reconnect button
   - Toast notifications for updates

6. **Performance:**
   - Disable debug logging in production
   - Use message batching for high frequency
   - Throttle UI updates if needed

---

**Next:** [Redis Pub/Sub Scaling](redis-pubsub-scaling.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
