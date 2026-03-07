/**
 * To-Be-Done Card Component
 *
 * Task management stored in localStorage.
 * Corresponds to Vue's home-to-be-done.vue
 */
import React, { useState, useCallback } from 'react';
import {
  Card,
  Checkbox,
  Input,
  Button,
  Space,
  List,
  Typography,
  Popconfirm,
} from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  StarOutlined,
  StarFilled,
  CheckSquareOutlined,
} from '@ant-design/icons';

const { Text } = Typography;

const STORAGE_KEY = 'smart_admin_todo';

interface TodoItem {
  title: string;
  doneFlag: boolean;
  starFlag: boolean;
}

function loadTodos(): TodoItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveTodos(todos: TodoItem[]): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(todos));
}

const ToBeDoneCard: React.FC = () => {
  const [todos, setTodos] = useState<TodoItem[]>(loadTodos);
  const [inputValue, setInputValue] = useState('');

  const updateTodos = useCallback((newTodos: TodoItem[]) => {
    // Sort: starred first, then by original order
    const sorted = [...newTodos].sort((a, b) => {
      if (a.starFlag === b.starFlag) return 0;
      return a.starFlag ? -1 : 1;
    });
    setTodos(sorted);
    saveTodos(sorted);
  }, []);

  const handleAdd = () => {
    if (!inputValue.trim()) return;
    updateTodos([...todos, { title: inputValue.trim(), doneFlag: false, starFlag: false }]);
    setInputValue('');
  };

  const handleToggle = (index: number) => {
    const newTodos = [...todos];
    newTodos[index] = { ...newTodos[index], doneFlag: !newTodos[index].doneFlag };
    updateTodos(newTodos);
  };

  const handleStar = (index: number) => {
    const newTodos = [...todos];
    newTodos[index] = { ...newTodos[index], starFlag: !newTodos[index].starFlag };
    updateTodos(newTodos);
  };

  const handleDelete = (index: number) => {
    const newTodos = todos.filter((_, i) => i !== index);
    updateTodos(newTodos);
  };

  const pendingTodos = todos.filter((t) => !t.doneFlag);
  const doneTodos = todos.filter((t) => t.doneFlag);

  return (
    <Card
      title={
        <Space size={8}>
          <CheckSquareOutlined style={{ color: '#52c41a' }} />
          <span>待辦事項</span>
          {pendingTodos.length > 0 && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              ({pendingTodos.length})
            </Text>
          )}
        </Space>
      }
      size="small"
      style={{ height: '100%' }}
    >
      {/* Add new task */}
      <Space.Compact style={{ width: '100%', marginBottom: 8 }}>
        <Input
          size="small"
          placeholder="添加待辦事項..."
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onPressEnter={handleAdd}
        />
        <Button size="small" type="primary" icon={<PlusOutlined />} onClick={handleAdd} />
      </Space.Compact>

      {/* Task list */}
      <div style={{ height: 240, overflowY: 'auto' }}>
        {/* Pending */}
        <List
          size="small"
          dataSource={pendingTodos}
          locale={{ emptyText: '暫無待辦事項' }}
          renderItem={(item) => {
            const realIndex = todos.indexOf(item);
            return (
              <List.Item style={{ padding: '2px 0' }}>
                <Space size={4} style={{ width: '100%', justifyContent: 'space-between' }}>
                  <Space size={4}>
                    <Checkbox
                      checked={false}
                      onChange={() => handleToggle(realIndex)}
                    />
                    <Text style={{ fontSize: 13 }}>{item.title}</Text>
                  </Space>
                  <Space size={2}>
                    <Button
                      type="text"
                      size="small"
                      icon={item.starFlag ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
                      onClick={() => handleStar(realIndex)}
                    />
                    <Popconfirm
                      title="確定刪除？"
                      onConfirm={() => handleDelete(realIndex)}
                      okText="確定"
                      cancelText="取消"
                    >
                      <Button type="text" size="small" icon={<DeleteOutlined />} danger />
                    </Popconfirm>
                  </Space>
                </Space>
              </List.Item>
            );
          }}
        />

        {/* Done */}
        {doneTodos.length > 0 && (
          <>
            <Text type="secondary" style={{ fontSize: 12, display: 'block', margin: '4px 0' }}>
              已完成 ({doneTodos.length})
            </Text>
            <List
              size="small"
              dataSource={doneTodos}
              renderItem={(item) => {
                const realIndex = todos.indexOf(item);
                return (
                  <List.Item style={{ padding: '2px 0' }}>
                    <Space size={4}>
                      <Checkbox
                        checked
                        onChange={() => handleToggle(realIndex)}
                      />
                      <Text delete type="secondary" style={{ fontSize: 13 }}>
                        {item.title}
                      </Text>
                    </Space>
                  </List.Item>
                );
              }}
            />
          </>
        )}
      </div>
    </Card>
  );
};

export default ToBeDoneCard;
