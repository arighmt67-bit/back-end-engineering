/* Pengelola pembuatan dan verifikasi JWT. */
const jwt = require('jsonwebtoken');
const { InvariantError } = require('../exceptions/errors');

const TokenManager = {
  /* ACCESS_TOKEN_AGE ditulis dalam DETIK pada berkas .env (misal 1800 = 30 menit).
     Nilai dari .env selalu bertipe string, sedangkan jsonwebtoken menafsirkan
     string angka polos sebagai MILIDETIK ("1800" -> 1 detik). Karena itu nilai
     numerik dikonversi lebih dulu ke Number agar dibaca sebagai detik, sementara
     format berdurasi seperti "30m" atau "1h" tetap diteruskan apa adanya. */
  generateAccessToken: (payload) => {
    const umur = process.env.ACCESS_TOKEN_AGE;
    const expiresIn = umur && /^\d+$/.test(String(umur).trim())
      ? Number(umur)
      : (umur || '30m');

    return jwt.sign(payload, process.env.ACCESS_TOKEN_KEY, { expiresIn });
  },

  generateRefreshToken: (payload) => jwt.sign(payload, process.env.REFRESH_TOKEN_KEY),

  /* Verifikasi refresh token. Token tidak valid -> 400 (InvariantError),
     sesuai perilaku yang diuji pada endpoint /authentications. */
  verifyRefreshToken: (refreshToken) => {
    try {
      return jwt.verify(refreshToken, process.env.REFRESH_TOKEN_KEY);
    } catch (error) {
      throw new InvariantError('Refresh token tidak valid');
    }
  },
};

module.exports = TokenManager;
