const path = require("path")
const MiniCssExtractPlugin = require("mini-css-extract-plugin")
const RemoveEmptyScriptsPlugin = require("webpack-remove-empty-scripts")
const CopyPlugin = require("copy-webpack-plugin")

module.exports = {
  mode: process.env.NODE_ENV,
  entry: {
    tubo: path.resolve(__dirname, "resources/src/styles/tubo.scss")
  },
  output: {
    path: path.resolve(__dirname, "resources/public")
  },
  plugins: [
    new RemoveEmptyScriptsPlugin(),
    new MiniCssExtractPlugin({
      filename: "styles/[name].css",
    }),
    new CopyPlugin({
      patterns: [
        {
          from: path.resolve(__dirname, "resources/src/images"),
          to: "images"
        }
      ]
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
