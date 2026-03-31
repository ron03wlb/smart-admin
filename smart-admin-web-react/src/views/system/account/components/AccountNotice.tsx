/**
 * Account Notice
 *
 * Corresponds to Vue's account/components/notice/index.vue
 * Wraps NoticeEmployeeList component (same pattern as Vue version).
 */
import React from 'react';
import NoticeEmployeeList from '@/views/business/oa/notice/NoticeEmployeeList';

const AccountNotice: React.FC = () => {
  return <NoticeEmployeeList />;
};

export default AccountNotice;
