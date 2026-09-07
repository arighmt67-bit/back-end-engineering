/* Layanan caching Redis. Seluruh operasi dibungkus try/catch agar kegagalan Redis
   tidak pernah menggagalkan permintaan HTTP (aplikasi jatuh kembali ke database). */
const redis = require('../config/redis');

const DEFAULT_TTL = 3600; // 1 jam, sesuai ketentuan kriteria

const cacheService = {
  async get(key) {
    try {
      return await redis.get(key);
    } catch (e) {
      console.error('[cache] gagal membaca:', e.message);
      return null;
    }
  },

  async set(key, value, ttl = DEFAULT_TTL) {
    try {
      await redis.set(key, value, 'EX', ttl);
    } catch (e) {
      console.error('[cache] gagal menulis:', e.message);
    }
  },

  async delete(...keys) {
    const daftar = keys.filter(Boolean);
    if (daftar.length === 0) return;
    try {
      await redis.del(...daftar);
    } catch (e) {
      console.error('[cache] gagal menghapus:', e.message);
    }
  },
};

/* Kunci cache terpusat agar penulisan dan penghapusan tidak pernah berbeda ejaan. */
const cacheKeys = {
  company: (id) => `company:${id}`,
  user: (id) => `user:${id}`,
  application: (id) => `application:${id}`,
  applicationsByUser: (userId) => `applications:user:${userId}`,
  applicationsByJob: (jobId) => `applications:job:${jobId}`,
  bookmarks: (userId) => `bookmarks:${userId}`,
};

module.exports = { cacheService, cacheKeys, DEFAULT_TTL };
