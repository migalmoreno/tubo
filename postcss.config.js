module.exports = {
  plugins: [
    require("postcss-import"),
    require("postcss-url")([
      {
        filter: "**/webfonts/**",
        url: "copy",
        assetsPath: "fonts",
        useHash: true,
      },
      {
        filter: "**/files/*",
        url: "copy",
        assetsPath: "fonts",
        useHash: true,
      },
    ]),
    require("autoprefixer"),
    require("tailwindcss"),
    process.env.NODE_ENV === "production"
      ? require("cssnano")({ preset: "default" })
      : false,
  ],
};
