const path = require('path');
const { getDefaultConfig } = require('@expo/metro-config');
const pkg = require('../package.json');

const projectRoot = __dirname;
const packageRoot = path.resolve(projectRoot, '..');
const config = getDefaultConfig(projectRoot);

config.watchFolders = [packageRoot];
config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, 'node_modules'),
  path.resolve(packageRoot, 'node_modules'),
];
config.resolver.extraNodeModules = {
  [pkg.name]: path.join(packageRoot, pkg.source),
  react: path.resolve(projectRoot, 'node_modules/react'),
  'react-native': path.resolve(projectRoot, 'node_modules/react-native'),
};

module.exports = config;
