/**
 * Menu API Unit Tests
 * 菜單 API 單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { menuApi } from './menuApi';
import type { MenuVO, MenuAddForm, MenuUpdateForm } from '@/views/system/menu/types';
import { MenuTypeEnum, PermsTypeEnum } from '@/views/system/menu/types';
import type { ResponseDTO } from '@/api/types/response';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import request from '@/utils/request';

describe('menuApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ==================== queryMenu Tests ====================

  describe('queryMenu', () => {
    it('should call GET /menu/query', async () => {
      const mockResponse: ResponseDTO<MenuVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            menuId: 1,
            menuName: '系統管理',
            menuType: MenuTypeEnum.CATALOG,
            parentId: 0,
            icon: 'SettingOutlined',
            sort: 100,
            visibleFlag: true,
            cacheFlag: false,
            disabledFlag: false,
            frameFlag: false,
          },
          {
            menuId: 2,
            menuName: '用戶管理',
            menuType: MenuTypeEnum.MENU,
            parentId: 1,
            icon: 'UserOutlined',
            path: '/system/user',
            component: '/system/user/index',
            sort: 10,
            visibleFlag: true,
            cacheFlag: true,
            disabledFlag: false,
            frameFlag: false,
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await menuApi.queryMenu();

      expect(request.get).toHaveBeenCalledWith('/menu/query');
      expect(result).toEqual(mockResponse);
      expect(result.data).toHaveLength(2);
    });

    it('should handle empty menu list', async () => {
      const mockResponse: ResponseDTO<MenuVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await menuApi.queryMenu();

      expect(result.data).toHaveLength(0);
    });
  });

  // ==================== queryMenuTree Tests ====================

  describe('queryMenuTree', () => {
    it('should call GET /menu/tree with onlyMenu parameter', async () => {
      const mockResponse: ResponseDTO<MenuVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            menuId: 1,
            menuName: '系統管理',
            menuType: MenuTypeEnum.CATALOG,
            parentId: 0,
            sort: 100,
            children: [
              {
                menuId: 2,
                menuName: '用戶管理',
                menuType: MenuTypeEnum.MENU,
                parentId: 1,
                path: '/system/user',
                sort: 10,
              },
            ],
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await menuApi.queryMenuTree(true);

      expect(request.get).toHaveBeenCalledWith('/menu/tree?onlyMenu=true');
      expect(result.data[0].children).toHaveLength(1);
    });

    it('should default to onlyMenu=false when parameter not provided', async () => {
      const mockResponse: ResponseDTO<MenuVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      await menuApi.queryMenuTree();

      expect(request.get).toHaveBeenCalledWith('/menu/tree?onlyMenu=false');
    });
  });

  // ==================== addMenu Tests ====================

  describe('addMenu', () => {
    it('should call POST /menu/add for catalog type', async () => {
      const addForm: MenuAddForm = {
        menuName: '新目錄',
        menuType: MenuTypeEnum.CATALOG,
        parentId: 0,
        icon: 'FolderOutlined',
        sort: 50,
        visibleFlag: true,
        disabledFlag: false,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await menuApi.addMenu(addForm);

      expect(request.post).toHaveBeenCalledWith('/menu/add', addForm);
      expect(result.ok).toBe(true);
    });

    it('should call POST /menu/add for menu type', async () => {
      const addForm: MenuAddForm = {
        menuName: '新菜單',
        menuType: MenuTypeEnum.MENU,
        parentId: 1,
        icon: 'MenuOutlined',
        path: '/new/menu',
        component: '/new/menu/index',
        cacheFlag: true,
        frameFlag: false,
        sort: 20,
        visibleFlag: true,
        disabledFlag: false,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await menuApi.addMenu(addForm);

      expect(request.post).toHaveBeenCalledWith('/menu/add', addForm);
      expect(result.ok).toBe(true);
    });

    it('should call POST /menu/add for points type', async () => {
      const addForm: MenuAddForm = {
        menuName: '新增功能點',
        menuType: MenuTypeEnum.POINTS,
        parentId: 2,
        contextMenuId: 2,
        permsType: PermsTypeEnum.SA_TOKEN,
        webPerms: 'system:user:add',
        apiPerms: 'system:user:add',
        sort: 1,
        disabledFlag: false,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await menuApi.addMenu(addForm);

      expect(request.post).toHaveBeenCalledWith('/menu/add', addForm);
      expect(result.ok).toBe(true);
    });
  });

  // ==================== updateMenu Tests ====================

  describe('updateMenu', () => {
    it('should call POST /menu/update', async () => {
      const updateForm: MenuUpdateForm = {
        menuId: 5,
        menuName: '更新後的菜單',
        menuType: MenuTypeEnum.MENU,
        parentId: 1,
        path: '/updated/menu',
        component: '/updated/menu/index',
        sort: 30,
        visibleFlag: true,
        cacheFlag: false,
        disabledFlag: false,
        frameFlag: false,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await menuApi.updateMenu(updateForm);

      expect(request.post).toHaveBeenCalledWith('/menu/update', updateForm);
      expect(result.ok).toBe(true);
    });

    it('should handle external link menu update', async () => {
      const updateForm: MenuUpdateForm = {
        menuId: 6,
        menuName: '外鏈菜單',
        menuType: MenuTypeEnum.MENU,
        parentId: 1,
        frameFlag: true,
        frameUrl: 'https://example.com',
        sort: 15,
        visibleFlag: true,
        disabledFlag: false,
      };

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.post).mockResolvedValue(mockResponse);

      const result = await menuApi.updateMenu(updateForm);

      expect(result.ok).toBe(true);
    });
  });

  // ==================== batchDeleteMenu Tests ====================

  describe('batchDeleteMenu', () => {
    it('should call GET /menu/batchDelete with ID list', async () => {
      const menuIdList = [10, 11, 12];

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await menuApi.batchDeleteMenu(menuIdList);

      expect(request.get).toHaveBeenCalledWith('/menu/batchDelete?menuIdList=10,11,12');
      expect(result.ok).toBe(true);
    });

    it('should handle single menu delete', async () => {
      const menuIdList = [99];

      const mockResponse: ResponseDTO<void> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: undefined,
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await menuApi.batchDeleteMenu(menuIdList);

      expect(request.get).toHaveBeenCalledWith('/menu/batchDelete?menuIdList=99');
      expect(result.ok).toBe(true);
    });
  });

  // ==================== getAuthUrl Tests ====================

  describe('getAuthUrl', () => {
    it('should call GET /menu/auth/url', async () => {
      const mockResponse: ResponseDTO<string[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          '/system/user/add',
          '/system/user/update',
          '/system/user/delete',
          '/system/role/query',
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await menuApi.getAuthUrl();

      expect(request.get).toHaveBeenCalledWith('/menu/auth/url');
      expect(result.data).toHaveLength(4);
      expect(result.data).toContain('/system/user/add');
    });

    it('should handle empty auth URL list', async () => {
      const mockResponse: ResponseDTO<string[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await menuApi.getAuthUrl();

      expect(result.data).toHaveLength(0);
    });
  });

  // ==================== Error Handling Tests ====================

  describe('Error Handling', () => {
    it('should handle API errors in queryMenu', async () => {
      const mockError = new Error('Network Error');
      vi.mocked(request.get).mockRejectedValue(mockError);

      await expect(menuApi.queryMenu()).rejects.toThrow('Network Error');
    });

    it('should handle API errors in addMenu', async () => {
      const addForm: MenuAddForm = {
        menuName: '測試',
        menuType: MenuTypeEnum.CATALOG,
        parentId: 0,
        sort: 0,
      };

      const mockError = new Error('Validation Error');
      vi.mocked(request.post).mockRejectedValue(mockError);

      await expect(menuApi.addMenu(addForm)).rejects.toThrow('Validation Error');
    });

    it('should handle API errors in batchDeleteMenu', async () => {
      const mockError = new Error('Menu has children');
      vi.mocked(request.get).mockRejectedValue(mockError);

      await expect(menuApi.batchDeleteMenu([1])).rejects.toThrow('Menu has children');
    });
  });

  // ==================== Tree Structure Tests ====================

  describe('Tree Structure Data', () => {
    it('should handle nested menu tree correctly', async () => {
      const mockResponse: ResponseDTO<MenuVO[]> = {
        code: 200,
        msg: 'Success',
        ok: true,
        data: [
          {
            menuId: 1,
            menuName: '系統管理',
            menuType: MenuTypeEnum.CATALOG,
            parentId: 0,
            sort: 100,
            children: [
              {
                menuId: 2,
                menuName: '用戶管理',
                menuType: MenuTypeEnum.MENU,
                parentId: 1,
                path: '/system/user',
                sort: 10,
                children: [
                  {
                    menuId: 3,
                    menuName: '新增用戶',
                    menuType: MenuTypeEnum.POINTS,
                    parentId: 2,
                    webPerms: 'system:user:add',
                    sort: 1,
                  },
                ],
              },
            ],
          },
        ],
      };

      vi.mocked(request.get).mockResolvedValue(mockResponse);

      const result = await menuApi.queryMenuTree();

      expect(result.data[0].children).toHaveLength(1);
      expect(result.data[0].children![0].children).toHaveLength(1);
      expect(result.data[0].children![0].children![0].menuName).toBe('新增用戶');
    });
  });
});
