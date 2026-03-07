/**
 * TagNav Multi-Tab Component (Default Style)
 *
 * Features:
 * 1. Auto-add tab on route navigation
 * 2. Click tab to switch page
 * 3. Close tabs (single, others, all)
 * 4. Right-click context menu
 * 5. Title resolved from menu tree
 */
import React, { useEffect } from 'react';
import { Tabs, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { selectMenuTree } from '@/store/slices/userSlice';
import type { MenuItem } from '@/types/user.types';
import {
  selectTagList,
  selectActiveKey,
  addTag,
  setActiveKey,
  removeTag,
  removeOtherTags,
  removeAllTags,
} from '@/store/slices/tagNavSlice';

/**
 * Find menu item by path in the menu tree (recursive)
 */
function findMenuByPath(menuList: MenuItem[], pathname: string): MenuItem | null {
  const normalizedPath = pathname.replace(/\/$/, '') || '/';

  for (const menu of menuList) {
    if (menu.children && menu.children.length > 0) {
      const found = findMenuByPath(menu.children, pathname);
      if (found) return found;
    }

    if (menu.path) {
      const menuPath = (menu.path.startsWith('/') ? menu.path : `/${menu.path}`).replace(/\/$/, '');
      if (normalizedPath === menuPath) {
        return menu;
      }
    }
  }
  return null;
}

const TagNav: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const tagList = useAppSelector(selectTagList);
  const activeKey = useAppSelector(selectActiveKey);
  const menuTree = useAppSelector(selectMenuTree);

  // Auto-add tab on route change
  useEffect(() => {
    const path = location.pathname;

    // Skip login and home pages
    if (path === '/login' || path === '/home' || path === '/') return;

    // Resolve title from menu tree
    const menuItem = findMenuByPath(menuTree, path);
    const title = menuItem?.menuName || path.split('/').pop() || path;

    dispatch(
      addTag({
        tag: {
          path,
          title,
          icon: menuItem?.icon,
        },
        fromPath: undefined,
      })
    );
  }, [location.pathname, dispatch, menuTree]);

  // Click tab to switch page
  const handleTabClick = (key: string) => {
    dispatch(setActiveKey(key));
    navigate(key);
  };

  // Handle tab edit (close)
  const handleEdit = (
    targetKey: React.MouseEvent | React.KeyboardEvent | string,
    action: 'add' | 'remove'
  ) => {
    if (action !== 'remove') return;
    const path = targetKey as string;
    handleClose(path);
  };

  // Smart close navigation
  const handleClose = (path: string) => {
    const index = tagList.findIndex((t) => t.path === path);
    if (index === -1) return;

    const closingTag = tagList[index];
    const isActive = activeKey === path;

    dispatch(removeTag(path));

    if (isActive) {
      // Priority: fromPath → left tab → right tab → home
      if (closingTag.fromPath && tagList.some((t) => t.path === closingTag.fromPath)) {
        navigate(closingTag.fromPath);
        dispatch(setActiveKey(closingTag.fromPath));
      } else if (index > 0) {
        const leftTag = tagList[index - 1];
        navigate(leftTag.path);
        dispatch(setActiveKey(leftTag.path));
      } else if (tagList.length > 1) {
        const rightTag = tagList[1];
        navigate(rightTag.path);
        dispatch(setActiveKey(rightTag.path));
      } else {
        navigate('/home');
      }
    }
  };

  // Right-click context menu items
  const getContextMenuItems = (path: string): MenuProps['items'] => [
    {
      key: 'close',
      label: '關閉',
      onClick: () => handleClose(path),
    },
    {
      key: 'closeOthers',
      label: '關閉其他',
      onClick: () => {
        dispatch(removeOtherTags(path));
        navigate(path);
        dispatch(setActiveKey(path));
      },
    },
    {
      key: 'closeAll',
      label: '關閉所有',
      onClick: () => {
        dispatch(removeAllTags());
        navigate('/home');
      },
    },
  ];

  if (tagList.length === 0) return null;

  return (
    <div
      style={{
        background: '#fff',
        padding: '0 12px',
        borderBottom: '1px solid #f0f0f0',
      }}
      data-testid="tag-nav"
    >
      <Tabs
        type="editable-card"
        hideAdd
        activeKey={activeKey}
        onTabClick={handleTabClick}
        onEdit={handleEdit}
        size="small"
        items={tagList.map((tag) => ({
          key: tag.path,
          label: (
            <Dropdown
              menu={{ items: getContextMenuItems(tag.path) }}
              trigger={['contextMenu']}
            >
              <span>{tag.title}</span>
            </Dropdown>
          ),
          closable: true,
        }))}
      />
    </div>
  );
};

export default TagNav;
