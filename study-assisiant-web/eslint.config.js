import js from "@eslint/js"
import pluginVue from "eslint-plugin-vue"
import globals from "globals"
import tseslint from "typescript-eslint"

export default tseslint.config(
  {
    ignores: [
      "dist/**",
      "node_modules/**",
      "coverage/**",
      "*.min.js"
    ]
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  // 只启用「正确性」规则，格式交给 Prettier，避免两套规则互相打架
  ...pluginVue.configs["flat/essential"],
  {
    files: ["**/*.vue"],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser
      }
    }
  },
  {
    files: ["**/*.{js,ts,vue}"],
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.node
      }
    },
    rules: {
      // 单文件页面组件不需要多单词命名
      "vue/multi-word-component-names": "off",
      // 允许 catch 里不使用的错误变量
      "no-unused-vars": ["error", {
        argsIgnorePattern: "^_",
        caughtErrors: "none"
      }],
      "@typescript-eslint/no-unused-vars": ["error", {
        argsIgnorePattern: "^_",
        caughtErrors: "none"
      }]
    }
  },
  {
    // Vite 脚手架生成的类型声明文件，放开 any 相关规则
    files: ["**/*.d.ts"],
    rules: {
      "@typescript-eslint/no-explicit-any": "off",
      "@typescript-eslint/no-empty-object-type": "off"
    }
  }
)
