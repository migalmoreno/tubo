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
      containers: {
        sm: "22rem",
      },
    },
  },
  plugins: [
    require("@tailwindcss/container-queries"),
    require("@tailwindcss/forms"),
    require("tailwind-scrollbar"),
  ],
};
