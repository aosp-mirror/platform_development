const shared = {
  printWidth: 80,
  tabWidth: 2,
  useTabs: false,
  semi: true,
  singleQuote: true,
  quoteProps: 'preserve',
  bracketSpacing: false,
  trailingComma: 'all',
  arrowParens: 'always',
  embeddedLanguageFormatting: 'off',
  bracketSameLine: true,
  singleAttributePerLine: false,
  jsxSingleQuote: false,
  htmlWhitespaceSensitivity: 'strict',
};

module.exports = {
  overrides: [
    {
      /** TSX/TS/JS-specific configuration. */
      files: '*.tsx',
      options: shared,
    },
    {
      files: '*.ts',
      options: shared,
    },
    {
      files: '*.js',
      options: shared,
    },
    {
      /** Sass-specific configuration. */
      files: '*.scss',
      options: {
        singleQuote: true,
      },
    },
    {
      files: '*.html',
      options: {
        printWidth: 100,
      },
    },
    {
      files: '*.acx.html',
      options: {
        parser: 'angular',
        singleQuote: true,
      },
    },
  ],
};
