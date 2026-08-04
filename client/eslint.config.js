import globals from 'globals';
import pluginJs from '@eslint/js';
import tseslint from 'typescript-eslint';
import pluginVue from 'eslint-plugin-vue';
import prettierConfig from 'eslint-config-prettier';

export default tseslint.config(
  // Files to lint
  { files: ['**/*.{js,mjs,cjs,ts,vue}'] },

  // Browser globals
  { languageOptions: { globals: globals.browser } },

  // Base JS rules
  pluginJs.configs.recommended,

  // TypeScript rules
  ...tseslint.configs.recommended,

  // Vue rules (flat/recommended includes essential + strongly-recommended)
  ...pluginVue.configs['flat/recommended'],

  // Tell TypeScript ESLint to parse <script> blocks in .vue files
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: { parser: tseslint.parser },
    },
  },

  // Disable ESLint rules that conflict with Prettier (must be last)
  prettierConfig,

  // Project-level overrides
  {
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },

  // Ignored paths
  {
    ignores: ['dist/', 'coverage/', 'node_modules/'],
  }
);
