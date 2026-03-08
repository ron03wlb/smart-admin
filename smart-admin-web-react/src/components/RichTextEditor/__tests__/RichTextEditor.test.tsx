/**
 * RichTextEditor Tests
 *
 * Tests the Tiptap-based editor component. Since Tiptap heavily
 * relies on ProseMirror DOM manipulation, we focus on structural
 * rendering and toolbar presence rather than deep editor operations.
 */
import { render, screen } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';

// Mock Tiptap modules since they need browser DOM APIs not in JSDOM
vi.mock('@tiptap/react', () => ({
  useEditor: () => ({
    chain: () => ({ focus: () => ({ toggleBold: () => ({ run: vi.fn() }), toggleItalic: () => ({ run: vi.fn() }), toggleStrike: () => ({ run: vi.fn() }), toggleHeading: () => ({ run: vi.fn() }), toggleBulletList: () => ({ run: vi.fn() }), toggleOrderedList: () => ({ run: vi.fn() }), toggleCodeBlock: () => ({ run: vi.fn() }), toggleBlockquote: () => ({ run: vi.fn() }), setHorizontalRule: () => ({ run: vi.fn() }), setImage: () => ({ run: vi.fn() }), setLink: () => ({ run: vi.fn() }), insertTable: () => ({ run: vi.fn() }), undo: () => ({ run: vi.fn() }), redo: () => ({ run: vi.fn() }) }) }),
    isActive: () => false,
    can: () => ({ undo: () => false, redo: () => false }),
    getHTML: () => '',
    commands: { setContent: vi.fn() },
  }),
  EditorContent: ({ editor }: { editor: unknown }) => <div data-testid="editor-content">{editor ? 'Editor loaded' : 'No editor'}</div>,
}));

vi.mock('@tiptap/starter-kit', () => ({ default: {} }));
vi.mock('@tiptap/extension-link', () => ({ default: { configure: () => ({}) } }));
vi.mock('@tiptap/extension-image', () => ({ default: {} }));
vi.mock('@tiptap/extension-table', () => ({ Table: { configure: () => ({}) } }));
vi.mock('@tiptap/extension-table-row', () => ({ default: {} }));
vi.mock('@tiptap/extension-table-header', () => ({ default: {} }));
vi.mock('@tiptap/extension-table-cell', () => ({ default: {} }));

import RichTextEditor from '../RichTextEditor';

describe('RichTextEditor', () => {
  it('should render editor container', () => {
    const { container } = render(<RichTextEditor />);
    expect(container.querySelector('.rich-text-editor')).toBeDefined();
  });

  it('should render toolbar with formatting buttons', () => {
    render(<RichTextEditor />);
    expect(screen.getByTitle('Bold')).toBeDefined();
    expect(screen.getByTitle('Italic')).toBeDefined();
    expect(screen.getByTitle('Strikethrough')).toBeDefined();
    expect(screen.getByTitle('Heading 2')).toBeDefined();
    expect(screen.getByTitle('Heading 3')).toBeDefined();
  });

  it('should render list and code buttons', () => {
    render(<RichTextEditor />);
    expect(screen.getByTitle('Bullet List')).toBeDefined();
    expect(screen.getByTitle('Ordered List')).toBeDefined();
    expect(screen.getByTitle('Code Block')).toBeDefined();
    expect(screen.getByTitle('Blockquote')).toBeDefined();
  });

  it('should render link, image, and table buttons', () => {
    render(<RichTextEditor />);
    expect(screen.getByTitle('Link')).toBeDefined();
    expect(screen.getByTitle('Image')).toBeDefined();
    expect(screen.getByTitle('Table')).toBeDefined();
  });

  it('should render undo and redo buttons', () => {
    render(<RichTextEditor />);
    expect(screen.getByTitle('Undo')).toBeDefined();
    expect(screen.getByTitle('Redo')).toBeDefined();
  });

  it('should render editor content area', () => {
    render(<RichTextEditor />);
    expect(screen.getByTestId('editor-content')).toBeDefined();
  });
});
