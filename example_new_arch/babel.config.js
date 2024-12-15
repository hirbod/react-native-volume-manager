const path = require('path');
const pak = require('../package.json');

module.exports = (api) => {
  api.cache(true)
  return {
    presets: [
      [
        'babel-preset-expo',
        {
          jsxRuntime: 'automatic',
          lazyImports: true,
        },
      ],
    ],
    plugins: [
          [
      'module-resolver',
      {
        extensions: ['.tsx', '.ts', '.js', '.json'],
        alias: {
            [pak.name]: path.join(__dirname, '..', pak.source),
          },
        },
      ],
      ['@babel/plugin-transform-flow-strip-types'],
      ['@babel/plugin-transform-class-properties', { loose: true }],
      ['@babel/plugin-transform-private-methods', { loose: true }],
      '@babel/plugin-proposal-export-namespace-from',
    ],
  }
}