import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';
import prettier from 'eslint-plugin-prettier';
import prettierConfig from 'eslint-config-prettier';

export default tseslint.config(
  {
    ignores: ['dist', 'node_modules', '.vite', 'coverage'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: {
        ...globals.browser,
        ...globals.es2021,
        React: 'readonly',
      },
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        ecmaFeatures: {
          jsx: true,
        },
      },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      prettier: prettier,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      ...prettierConfig.rules,

      // React Hooks 規則
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',

      // React Refresh 規則
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true },
      ],

      // TypeScript 規則
      '@typescript-eslint/no-explicit-any': 'warn', // TODO: Week 3 修復所有 any 類型
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
        },
      ],
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      '@typescript-eslint/no-non-null-assertion': 'warn',

      // 一般規則
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'error',
      'no-alert': 'warn',
      'prefer-const': 'error',
      'no-var': 'error',

      // Prettier 規則
      'prettier/prettier': 'error',
    },
  },
  // 測試文件和配置文件特殊規則
  {
    files: ['**/*.test.{ts,tsx}', '**/*.spec.{ts,tsx}', '**/test/**/*.{ts,tsx}', '**/*.config.{ts,js}'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.es2021,
        React: 'readonly',
      },
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'warn', // 測試文件放寬 any 限制
    },
  }
);
