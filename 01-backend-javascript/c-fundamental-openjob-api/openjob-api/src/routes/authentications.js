/* Route resource authentications (login, refresh, logout). */
const express = require('express');
const bcrypt = require('bcryptjs');
const pool = require('../config/database');
const validate = require('../middlewares/validate');
const auth = require('../middlewares/auth');
const asyncHandler = require('../utils/asyncHandler');
const TokenManager = require('../utils/tokenManager');
const {
  LoginPayloadSchema,
  RefreshTokenPayloadSchema,
  DeleteAuthenticationPayloadSchema,
} = require('../validators/schemas');
const { InvariantError, AuthenticationError } = require('../exceptions/errors');

const router = express.Router();

/* POST /authentications - login */
router.post('/', validate(LoginPayloadSchema), asyncHandler(async (req, res) => {
  const { email, password } = req.body;

  const result = await pool.query('SELECT id, password FROM users WHERE email = $1', [email]);
  if (result.rowCount === 0) {
    throw new AuthenticationError('Kredensial yang Anda berikan salah');
  }

  const { id, password: hashedPassword } = result.rows[0];
  const match = await bcrypt.compare(password, hashedPassword);
  if (!match) {
    throw new AuthenticationError('Kredensial yang Anda berikan salah');
  }

  const accessToken = TokenManager.generateAccessToken({ id });
  const refreshToken = TokenManager.generateRefreshToken({ id });

  await pool.query('INSERT INTO authentications (token) VALUES ($1)', [refreshToken]);

  return res.status(200).json({
    status: 'success',
    message: 'Authentication berhasil ditambahkan',
    data: { accessToken, refreshToken },
  });
}));

/* PUT /authentications - memperbarui access token */
router.put('/', validate(RefreshTokenPayloadSchema), asyncHandler(async (req, res) => {
  const { refreshToken } = req.body;

  /* Refresh token wajib masih tersimpan di database (belum logout). */
  const stored = await pool.query('SELECT token FROM authentications WHERE token = $1', [refreshToken]);
  if (stored.rowCount === 0) {
    throw new InvariantError('Refresh token tidak valid');
  }

  const { id } = TokenManager.verifyRefreshToken(refreshToken);
  const accessToken = TokenManager.generateAccessToken({ id });

  return res.status(200).json({
    status: 'success',
    message: 'Access Token berhasil diperbarui',
    data: { accessToken },
  });
}));

/* DELETE /authentications - logout (protected: wajib akses token valid) */
router.delete('/', auth, validate(DeleteAuthenticationPayloadSchema), asyncHandler(async (req, res) => {
  const { refreshToken } = req.body;

  const stored = await pool.query('SELECT token FROM authentications WHERE token = $1', [refreshToken]);
  if (stored.rowCount === 0) {
    throw new InvariantError('Refresh token tidak valid');
  }

  await pool.query('DELETE FROM authentications WHERE token = $1', [refreshToken]);

  return res.status(200).json({ status: 'success', message: 'Refresh token berhasil dihapus' });
}));

module.exports = router;
