/* Middleware caching. Menyisipkan header X-Data-Source agar sumber data terlihat jelas:
   - 'cache'    -> jawaban berasal dari Redis
   - 'database' -> jawaban berasal dari PostgreSQL, lalu disimpan ke Redis */
const { cacheService, DEFAULT_TTL } = require('../services/cacheService');

const cache = (buatKunci, ttl = DEFAULT_TTL) => async (req, res, next) => {
  const key = buatKunci(req);

  const tersimpan = await cacheService.get(key);
  if (tersimpan) {
    res.set('X-Data-Source', 'cache');
    return res.status(200).json(JSON.parse(tersimpan));
  }

  res.set('X-Data-Source', 'database');

  /* Menyimpan hasil hanya bila permintaan benar-benar berhasil (200). */
  const kirimAsli = res.json.bind(res);
  res.json = (body) => {
    if (res.statusCode === 200) {
      cacheService.set(key, JSON.stringify(body), ttl);
    }
    return kirimAsli(body);
  };

  return next();
};

module.exports = cache;
