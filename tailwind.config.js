/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,js,cljs}"],
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        nunito: ['nunito-light', 'sans-serif'],
        roboto: ['roboto-light', 'sans-serif'],
      },
      screens: {
        'xs': '480px',
        'ml': '930px',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms')
  ],
}
