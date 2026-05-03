module.exports = {
  plugins: [
    require("postcss-import")({
      filter: (id) => !id.includes("tailwindcss"),
    }),
    require("@tailwindcss/postcss"),
    require("postcss-url")([
      {
        filter: "**/webfonts/**",
        url: "copy",
        assetsPath: "fonts",
        useHash: true,
      },
      {
        filter: "**/files/**",
        url: "copy",
        assetsPath: "fonts",
        useHash: true,
      },
    ]),
    process.env.NODE_ENV === "production"
      ? require("cssnano")({ preset: "default" })
      : false,
  ],
};
