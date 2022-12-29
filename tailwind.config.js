/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,js,cljs}"],
  theme: {
    extend: {
      fontFamily: {
        nunito: ['nunito-light', 'sans-serif'],
        roboto: ['roboto-light', 'sans-serif'],
      },
    },
  },
  plugins: [
    require('@tailwindcss/line-clamp'),
    require('@tailwindcss/forms')
  ],
}
