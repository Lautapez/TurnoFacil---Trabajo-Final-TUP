/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#10243A',
        tide: { DEFAULT: '#0E7C86', dark: '#0A5E66', soft: '#DDF0F1' },
        mist: '#EEF3F6',
        line: '#D5DEE5',
        muted: '#5A6B7B',
      },
      fontFamily: {
        display: ['"Bricolage Grotesque"', 'system-ui', 'sans-serif'],
        sans: ['Figtree', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
