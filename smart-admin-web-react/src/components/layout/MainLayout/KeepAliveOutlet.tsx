/**
 * Keep-Alive Outlet (CSS display:none approach)
 *
 * Caches visited route components and keeps them mounted using display:none.
 * Only the currently active route is visible (display:block).
 * When a tag is removed from TagNav, the cached component is unmounted.
 *
 * Limit: MAX_KEEP_ALIVE (30 tabs, enforced by tagNavSlice)
 */
import React, { useRef } from 'react';
import { useOutlet, useLocation } from 'react-router-dom';
import { useAppSelector } from '@/store/hooks';
import { selectTagList } from '@/store/slices/tagNavSlice';

const KeepAliveOutlet: React.FC = () => {
  const outlet = useOutlet();
  const location = useLocation();
  const tagList = useAppSelector(selectTagList);

  // Cache: pathname → React element
  const cacheRef = useRef(new Map<string, React.ReactNode>());

  // Cache current outlet element
  if (outlet) {
    cacheRef.current.set(location.pathname, outlet);
  }

  // Clean up: remove cached elements no longer in tagList
  // Always keep /home cached since it's not tracked in tagList
  const activePaths = new Set(['/home', ...tagList.map((t) => t.path)]);
  for (const key of Array.from(cacheRef.current.keys())) {
    if (!activePaths.has(key)) {
      cacheRef.current.delete(key);
    }
  }

  return (
    <>
      {Array.from(cacheRef.current.entries()).map(([path, element]) => (
        <div
          key={path}
          style={{
            display: path === location.pathname ? 'block' : 'none',
            height: '100%',
          }}
        >
          {element}
        </div>
      ))}
    </>
  );
};

export default KeepAliveOutlet;
