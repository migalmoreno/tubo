module.exports = {
  content: ["./src/**/*.{html,js,cljs}"],
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        "nunito-sans": ["Nunito Sans", "sans-serif"],
      },
      screens: {
        xs: "480px",
      },
    },
  },
  plugins: [require("@tailwindcss/forms"), require("tailwind-scrollbar")],
};
