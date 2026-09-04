module.exports = {
  "root": true,
  "parser": "vue-eslint-parser",
  "parserOptions": {
    "parser": "espree",
    "ecmaVersion": 2020,
    "sourceType": "script"
  },
  "env": {
    "browser": true,
    "es6": true,
    "node": false
  },
  "extends": [
    "eslint:recommended"
  ],
  "plugins": [
    "html"
  ],
  "globals": {
    "Vue": "readonly",
    "axios": "readonly",
    "ELEMENT": "readonly",
    "echarts": "readonly",
    "ReggieUI": "readonly",
    "ReggieListMixin": "readonly",
    "ReggieStatus": "readonly",
    "RgPalette": "readonly",
    "RgFormat": "readonly",
    "REGGIE": "readonly",
    "mixin": "readonly",
    "Handlebars": "readonly"
  },
  "rules": {
    "no-console": "off",
    "no-debugger": "off",
    "no-unused-vars": [
      "warn",
      {
        "vars": "all",
        "args": "after-used",
        "ignoreRestSiblings": true
      }
    ],
    "no-empty": [
      "error",
      {
        "allowEmptyCatch": true
      }
    ],
    "no-alert": "warn",
    "no-eval": "error",
    "no-implied-eval": "error",
    "no-new-func": "error",
    "no-redeclare": "warn",
    "no-undef": "warn",
    "no-prototype-builtins": "off"
  }
}
