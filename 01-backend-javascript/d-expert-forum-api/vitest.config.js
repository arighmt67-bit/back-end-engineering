import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    /**
     * Berkas setup ini memuat .test.env dengan override sehingga pengujian
     * selalu memakai basis data pengujian. Jika hanya memakai 'dotenv/config',
     * berkas .env (basis data pengembangan) yang termuat dan pengujian akan
     * menulis ke data pengembangan.
     */
    setupFiles: ['./tests/setupEnv.js'],
    /**
     * Pengujian integration dan functional berbagi satu basis data.
     * Menjalankan berkas uji secara berurutan mencegah tumpang tindih data antar berkas.
     */
    fileParallelism: false,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: ['src/**'],
      exclude: [
        'src/**/_test/**',
        'src/Infrastructures/database/**',
        'src/app.js', // entry point, hanya mengikat port
        'src/Commons/config.js', // pemuatan environment variable
      ],
    },
  },
});
