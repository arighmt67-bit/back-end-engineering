/* Konfigurasi koneksi Redis. Kredensial dibaca dari environment variables. */
const Redis = require('ioredis');

const client = new Redis({
  host: process.env.REDIS_HOST || 'localhost',
  port: Number(process.env.REDIS_PORT) || 6379,
  password: process.env.REDIS_PASSWORD || undefined,
  lazyConnect: false,
  maxRetriesPerRequest: 1,
  /* Aplikasi tetap melayani permintaan walau Redis sedang tidak tersedia. */
  retryStrategy: (times) => Math.min(times * 200, 2000),
});

client.on('error', (e) => {
  console.error('[redis] koneksi bermasalah:', e.message);
});

module.exports = client;
