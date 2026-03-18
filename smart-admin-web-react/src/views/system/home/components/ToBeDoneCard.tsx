/**
 * To Be Done Card - 待辦事項卡片
 * 顯示待辦工作列表，支持本地存儲、CRUD 操作和星標排序
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/components/to-be-done-card/home-to-be-done.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-17
 */

import { useState, useEffect } from 'react';
import { Checkbox, Button, Tooltip, Empty, Modal } from 'antd';
import {
  StarOutlined,
  StarFilled,
  DeleteOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import DefaultHomeCard from './DefaultHomeCard';
import ToBeDoneModal from './ToBeDoneModal';
import type { ToBeDoneItem } from '../types';
import { LOCAL_STORAGE_KEYS } from '../types';
import './ToBeDoneCard.css';

const ToBeDoneCard: React.FC = () => {
  const [data, setData] = useState<ToBeDoneItem[]>([]);
  const [modalVisible, setModalVisible] = useState(false);

  /**
   * 從 localStorage 加載待辦事項
   */
  const loadFromLocalStorage = (): ToBeDoneItem[] => {
    try {
      const stored = localStorage.getItem(LOCAL_STORAGE_KEYS.TO_BE_DONE_LIST);
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (error) {
      console.error('加載待辦事項失敗:', error);
    }
    return [];
  };

  /**
   * 保存待辦事項到 localStorage
   */
  const saveToLocalStorage = (list: ToBeDoneItem[]) => {
    try {
      localStorage.setItem(LOCAL_STORAGE_KEYS.TO_BE_DONE_LIST, JSON.stringify(list));
    } catch (error) {
      console.error('保存待辦事項失敗:', error);
    }
  };

  /**
   * 排序待辦事項
   * 規則：未完成在前，已完成在後；未完成項中，星標優先
   */
  const sortToDos = (list: ToBeDoneItem[]): ToBeDoneItem[] => {
    return [...list].sort((a, b) => {
      // 1. 未完成項在前
      if (a.doneFlag !== b.doneFlag) {
        return a.doneFlag ? 1 : -1;
      }
      // 2. 未完成項中，星標優先
      if (!a.doneFlag && a.starFlag !== b.starFlag) {
        return b.starFlag ? 1 : -1;
      }
      // 3. 按創建時間排序（新的在前）
      return (b.createTime || '').localeCompare(a.createTime || '');
    });
  };

  /**
   * 初始化加載
   */
  useEffect(() => {
    const loaded = loadFromLocalStorage();
    setData(sortToDos(loaded));
  }, []);

  /**
   * 新增待辦事項
   */
  const handleAdd = (title: string) => {
    const newItem: ToBeDoneItem = {
      title,
      doneFlag: false,
      starFlag: false,
      createTime: new Date().toISOString(),
    };
    const newList = [newItem, ...data];
    const sorted = sortToDos(newList);
    setData(sorted);
    saveToLocalStorage(sorted);
  };

  /**
   * 切換完成狀態
   */
  const handleToggleDone = (index: number) => {
    const newList = [...data];
    newList[index].doneFlag = !newList[index].doneFlag;
    const sorted = sortToDos(newList);
    setData(sorted);
    saveToLocalStorage(sorted);
  };

  /**
   * 切換星標狀態
   */
  const handleToggleStar = (index: number) => {
    const newList = [...data];
    newList[index].starFlag = !newList[index].starFlag;
    const sorted = sortToDos(newList);
    setData(sorted);
    saveToLocalStorage(sorted);
  };

  /**
   * 刪除待辦事項
   * 已完成：直接刪除
   * 未完成：彈出確認框
   */
  const handleDelete = (index: number) => {
    const item = data[index];

    // 已完成項直接刪除
    if (item.doneFlag) {
      const newList = data.filter((_, i) => i !== index);
      setData(newList);
      saveToLocalStorage(newList);
      return;
    }

    // 未完成項需要確認
    Modal.confirm({
      title: '確認刪除',
      icon: <ExclamationCircleOutlined />,
      content: '此待辦事項尚未完成，確定要刪除嗎？',
      okText: '確認',
      cancelText: '取消',
      onOk: () => {
        const newList = data.filter((_, i) => i !== index);
        setData(newList);
        saveToLocalStorage(newList);
      },
    });
  };

  return (
    <DefaultHomeCard
      icon="StarTwoTone"
      title="待辦工作"
      extra="新增"
      onExtraClick={() => setModalVisible(true)}
    >
      <div className="to-be-done-box">
        {data.length === 0 ? (
          <Empty description="暫無待辦事項" />
        ) : (
          <ul className="to-be-done-list">
            {data.map((item, index) => (
              <li
                key={`${item.createTime}-${index}`}
                className={item.doneFlag ? 'done' : 'un-done'}
              >
                <Checkbox checked={item.doneFlag} onChange={() => handleToggleDone(index)}>
                  <span className={item.doneFlag ? 'done-text' : ''}>{item.title}</span>
                </Checkbox>
                <div className="actions">
                  {/* 星標按鈕 */}
                  <Tooltip title={item.starFlag ? '取消星標' : '設為星標'}>
                    <Button
                      type="text"
                      size="small"
                      icon={
                        item.starFlag ? (
                          <StarFilled style={{ color: '#faad14' }} />
                        ) : (
                          <StarOutlined />
                        )
                      }
                      onClick={() => handleToggleStar(index)}
                    />
                  </Tooltip>
                  {/* 刪除按鈕 */}
                  <Tooltip title="刪除">
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => handleDelete(index)}
                    />
                  </Tooltip>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* 新增待辦Modal */}
      <ToBeDoneModal
        visible={modalVisible}
        onClose={() => setModalVisible(false)}
        onSubmit={handleAdd}
      />
    </DefaultHomeCard>
  );
};

export default ToBeDoneCard;
