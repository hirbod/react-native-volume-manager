module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  parserOptions: {
    tsconfigRootDir: __dirname,
    project: ['./tsconfig.json'],
    ecmaFeatures: {
      jsx: true,
    },
    ecmaVersion: 2018,
    sourceType: 'module',
  },
  ignorePatterns: ['babel.config.js', 'metro.config.js', '.eslintrc.js'],
  plugins: ['@typescript-eslint'],
  extends: ['plugin:@typescript-eslint/recommended', '@react-native-community', 'plugin:react/jsx-runtime'],
  rules: {
    'react-native/no-inline-styles': 0,
    'react/no-unstable-nested-components': 0,
  }
};
