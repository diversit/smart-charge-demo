import { defineConfig } from 'unocss'

export default defineConfig({
  shortcuts: {
    // custom the default background
    'bg-main': 'bg-white text-[#181818] dark:(bg-[#121212] text-[#ddd])',
    'bg-barbottom': 'bg-amber-500 text-amber-50 dark:(bg-amber-500 text-amber-50)',
},
  // ...
})