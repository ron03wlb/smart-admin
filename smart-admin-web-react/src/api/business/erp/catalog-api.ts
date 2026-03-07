import { getRequest, postRequest } from '@/api/base/request';

export interface CategoryVO {
  categoryId: number;
  categoryName: string;
  categoryType: number;
  parentId: number;
  sort?: number;
  disabledFlag?: boolean;
  children?: CategoryVO[];
}

export interface CategoryAddForm {
  categoryName: string;
  categoryType: number;
  parentId: number;
  sort?: number;
}

export interface CategoryUpdateForm extends CategoryAddForm {
  categoryId: number;
}

export const categoryApi = {
  queryTree: (data: { categoryType: number }) =>
    postRequest<CategoryVO[]>('/category/tree', data),
  add: (data: CategoryAddForm) => postRequest<void>('/category/add', data),
  update: (data: CategoryUpdateForm) => postRequest<void>('/category/update', data),
  delete: (categoryId: number) => getRequest<void>(`/category/delete/${categoryId}`),
};
