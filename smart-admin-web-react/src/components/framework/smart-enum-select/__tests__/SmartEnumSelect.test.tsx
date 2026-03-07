import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SmartEnumSelect from '../SmartEnumSelect';

describe('SmartEnumSelect', () => {
  it('should render options from enum', () => {
    const { container } = render(<SmartEnumSelect enumName="FLAG_NUMBER_ENUM" />);
    // Select renders a container
    expect(container.querySelector('.ant-select')).toBeTruthy();
  });

  it('should render with value selected', () => {
    render(<SmartEnumSelect enumName="FLAG_NUMBER_ENUM" value={1} />);
    // Ant Design Select renders the selected label in a span
    expect(screen.getByTitle('是')).toBeTruthy();
  });

  it('should call onChange when value changes', async () => {
    const onChange = vi.fn();
    render(<SmartEnumSelect enumName="GENDER_ENUM" onChange={onChange} />);

    // Click to open dropdown
    const select = document.querySelector('.ant-select-selector')!;
    await userEvent.click(select);

    // Click an option
    const option = await screen.findByText('男');
    await userEvent.click(option);

    expect(onChange).toHaveBeenCalledWith(1, expect.anything());
  });

  it('should be disabled when disabled prop is true', () => {
    const { container } = render(<SmartEnumSelect enumName="FLAG_NUMBER_ENUM" disabled />);
    expect(container.querySelector('.ant-select-disabled')).toBeTruthy();
  });

  it('should hide options in hiddenOption list', async () => {
    render(<SmartEnumSelect enumName="GENDER_ENUM" hiddenOption={[0]} />);

    const select = document.querySelector('.ant-select-selector')!;
    await userEvent.click(select);

    // '未知' (value=0) should not appear
    expect(screen.queryByText('未知')).toBeNull();
    expect(await screen.findByText('男')).toBeTruthy();
    expect(screen.getByText('女')).toBeTruthy();
  });

  it('should clear value if it is in hiddenOption', () => {
    render(<SmartEnumSelect enumName="GENDER_ENUM" value={0} hiddenOption={[0]} />);
    // Should not show '未知' as selected
    expect(screen.queryByTitle('未知')).toBeNull();
  });
});
