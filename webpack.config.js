const path = require("path")
const MiniCssExtractPlugin = require("mini-css-extract-plugin")
const RemoveEmptyScriptsPlugin = require("webpack-remove-empty-scripts")

module.exports = {
  mode: process.env.NODE_ENV,
  entry: {
    tubo: "./resources/src/css/tubo.scss"
  },
  output: {
    path: path.resolve(__dirname, "resources/public")
  },
  plugins: [
    new RemoveEmptyScriptsPlugin(),
    new MiniCssExtractPlugin({
      filename: "css/[name].css",
    })
  ],
  module: {
    rules: [
      {
        test: /\.scss$/i,
        exclude: /node_modules/,
        use: [
          MiniCssExtractPlugin.loader,
          {
            loader: 'css-loader',
            options: {
              importLoaders: 1
            }
          },
          "postcss-loader",
          "sass-loader"
        ]
      },
      {
        test: /\.(woff|woff2|eot|ttf|otf)$/i,
        type: 'asset/resource',
        generator: {
          filename: 'fonts/[hash][ext][query]'
        }
      }
    ]
  }
}
