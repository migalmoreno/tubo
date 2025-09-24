import * as esbuild from "esbuild";

const config = {
  bundle: true,
  sourcemap: true,
  entryPoints: ["target/js/index.js"],
  outfile: "resources/public/js/libs.js",
  target: ["es2020", "chrome119", "firefox120", "edge119"],
  logLevel: "info",
};

const watch = async () => {
  let ctx = await esbuild.context(config);
  await ctx.watch();
};

if (process.argv.includes("--watch")) {
  await watch();
} else {
  await esbuild.build({ ...config, minify: false });
}
