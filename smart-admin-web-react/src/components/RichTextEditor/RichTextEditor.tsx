/**
 * RichTextEditor - Tiptap-based rich text editor for Ant Design Form
 *
 * Replaces plain TextArea with toolbar-equipped HTML editor.
 * Integrates with Ant Design Form via value/onChange props.
 */
import React, { useEffect } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Image from '@tiptap/extension-image';
import { Table } from '@tiptap/extension-table';
import TableRow from '@tiptap/extension-table-row';
import TableHeader from '@tiptap/extension-table-header';
import TableCell from '@tiptap/extension-table-cell';
import './RichTextEditor.css';

interface RichTextEditorProps {
  value?: string;
  onChange?: (html: string) => void;
  placeholder?: string;
}

const MenuBar: React.FC<{ editor: ReturnType<typeof useEditor> }> = ({ editor }) => {
  if (!editor) return null;

  const addImage = () => {
    const url = window.prompt('Image URL:');
    if (url) editor.chain().focus().setImage({ src: url }).run();
  };

  const addLink = () => {
    const url = window.prompt('Link URL:');
    if (url) editor.chain().focus().setLink({ href: url }).run();
  };

  const addTable = () => {
    editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run();
  };

  return (
    <div className="rich-text-toolbar">
      <button type="button" onClick={() => editor.chain().focus().toggleBold().run()} className={editor.isActive('bold') ? 'is-active' : ''} title="Bold">
        <strong>B</strong>
      </button>
      <button type="button" onClick={() => editor.chain().focus().toggleItalic().run()} className={editor.isActive('italic') ? 'is-active' : ''} title="Italic">
        <em>I</em>
      </button>
      <button type="button" onClick={() => editor.chain().focus().toggleStrike().run()} className={editor.isActive('strike') ? 'is-active' : ''} title="Strikethrough">
        <s>S</s>
      </button>
      <span className="rich-text-toolbar-divider" />
      <button type="button" onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()} className={editor.isActive('heading', { level: 2 }) ? 'is-active' : ''} title="Heading 2">
        H2
      </button>
      <button type="button" onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()} className={editor.isActive('heading', { level: 3 }) ? 'is-active' : ''} title="Heading 3">
        H3
      </button>
      <span className="rich-text-toolbar-divider" />
      <button type="button" onClick={() => editor.chain().focus().toggleBulletList().run()} className={editor.isActive('bulletList') ? 'is-active' : ''} title="Bullet List">
        UL
      </button>
      <button type="button" onClick={() => editor.chain().focus().toggleOrderedList().run()} className={editor.isActive('orderedList') ? 'is-active' : ''} title="Ordered List">
        OL
      </button>
      <span className="rich-text-toolbar-divider" />
      <button type="button" onClick={() => editor.chain().focus().toggleCodeBlock().run()} className={editor.isActive('codeBlock') ? 'is-active' : ''} title="Code Block">
        {'</>'}
      </button>
      <button type="button" onClick={() => editor.chain().focus().toggleBlockquote().run()} className={editor.isActive('blockquote') ? 'is-active' : ''} title="Blockquote">
        &ldquo;&rdquo;
      </button>
      <span className="rich-text-toolbar-divider" />
      <button type="button" onClick={addLink} className={editor.isActive('link') ? 'is-active' : ''} title="Link">
        Link
      </button>
      <button type="button" onClick={addImage} title="Image">
        Img
      </button>
      <button type="button" onClick={addTable} title="Table">
        Table
      </button>
      <span className="rich-text-toolbar-divider" />
      <button type="button" onClick={() => editor.chain().focus().setHorizontalRule().run()} title="Horizontal Rule">
        HR
      </button>
      <button type="button" onClick={() => editor.chain().focus().undo().run()} disabled={!editor.can().undo()} title="Undo">
        Undo
      </button>
      <button type="button" onClick={() => editor.chain().focus().redo().run()} disabled={!editor.can().redo()} title="Redo">
        Redo
      </button>
    </div>
  );
};

const RichTextEditor: React.FC<RichTextEditorProps> = ({ value, onChange, placeholder }) => {
  const editor = useEditor({
    extensions: [
      StarterKit,
      Link.configure({ openOnClick: false }),
      Image,
      Table.configure({ resizable: true }),
      TableRow,
      TableHeader,
      TableCell,
    ],
    content: value || '',
    onUpdate: ({ editor: e }) => {
      onChange?.(e.getHTML());
    },
    editorProps: {
      attributes: {
        class: 'rich-text-content',
        ...(placeholder ? { 'data-placeholder': placeholder } : {}),
      },
    },
  });

  // Sync external value changes (e.g., form.setFieldsValue)
  useEffect(() => {
    if (editor && value !== undefined && value !== editor.getHTML()) {
      editor.commands.setContent(value, { emitUpdate: false });
    }
  }, [value, editor]);

  return (
    <div className="rich-text-editor">
      <MenuBar editor={editor} />
      <EditorContent editor={editor} />
    </div>
  );
};

export default RichTextEditor;
