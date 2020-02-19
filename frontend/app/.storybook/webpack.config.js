const path = require('path');

module.exports = ({ config }) => {
  config.module.rules.push({
    test: /\.(ts|tsx)$/,
    use: [
      {
        loader: require.resolve('awesome-typescript-loader'),
        query: {
          configFileName: 'tsconfig.storybook.json',
        },
      },
    ],
  });

  // Required for absolute imports in Storybook
  config.resolve.modules.push(path.resolve(process.cwd(), 'src'));

  config.resolve.extensions.push('.ts', '.tsx');
  return config;
};