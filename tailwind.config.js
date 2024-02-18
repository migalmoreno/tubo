/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,js,cljs}"],
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        "nunito": ["nunito-light", "sans-serif"],
        "nunito-semibold": ["nunito-semibold", "sans-serif"],
        "nunito-bold": ["nunito-bold", "sans-serif"],
        "roboto": ["roboto-light", "sans-serif"],
        "roboto-medium": ["roboto-medium", "sans-serif"],
      },
      screens: {
        "xs": "480px",
      },
    },
  },
  plugins: [
    require("@tailwindcss/forms")
  ],
}
