/**
 * Text Ellipsis Component
 *
 * Corresponds to Vue's components/framework/text-ellipsis/index.vue
 * Shows truncated text with a Tooltip on hover when text overflows.
 */
import React, { useRef, useState, useEffect } from 'react';
import { Tooltip } from 'antd';

interface TextEllipsisProps {
  text: string;
  maxWidth?: string | number;
  style?: React.CSSProperties;
}

const TextEllipsis: React.FC<TextEllipsisProps> = ({ text, maxWidth = '100%', style }) => {
  const textRef = useRef<HTMLSpanElement>(null);
  const [isOverflow, setIsOverflow] = useState(false);

  useEffect(() => {
    const el = textRef.current;
    if (el) {
      setIsOverflow(el.scrollWidth > el.clientWidth);
    }
  }, [text]);

  const innerStyle: React.CSSProperties = {
    display: 'inline-block',
    maxWidth,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    verticalAlign: 'middle',
    ...style,
  };

  if (isOverflow) {
    return (
      <Tooltip title={text}>
        <span ref={textRef} style={innerStyle}>
          {text}
        </span>
      </Tooltip>
    );
  }

  return (
    <span ref={textRef} style={innerStyle}>
      {text}
    </span>
  );
};

export default TextEllipsis;
