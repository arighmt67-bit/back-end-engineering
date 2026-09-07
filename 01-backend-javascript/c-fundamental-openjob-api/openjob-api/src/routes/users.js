/* Route resource users. Detail pengguna disajikan lewat cache Redis. */
const express = require('express');
const bcrypt = require('bcryptjs');
const { nanoid } = require('nanoid');
const pool = require('../config/database');
const validate = require('../middlewares/validate');
const auth = require('../middlewares/auth');
const cache = require('../middlewares/cache');
const asyncHandler = require('../utils/asyncHandler');
const { UserPayloadSchema, UserUpdatePayloadSchema } = require('../validators/schemas');
const { InvariantError, NotFoundError, AuthorizationError } = require('../exceptions/errors');
const { cacheService, cacheKeys } = require('../services/cacheService');

const router = express.Router();

/* POST /users - registrasi pengguna baru */
router.post('/', validate(UserPayloadSchema), asyncHandler(async (req, res) => {
  const { name, email, password, role = 'user' } = req.body;

  const duplicate = await pool.query('SELECT id FROM users WHERE email = $1', [email]);
  if (duplicate.rowCount > 0) {
    throw new InvariantError('Email sudah digunakan');
  }

  const id = `user-${nanoid(16)}`;
  const hashedPassword = await bcrypt.hash(password, 10);

  const result = await pool.query(
    'INSERT INTO users (id, name, email, password, role) VALUES ($1, $2, $3, $4, $5) RETURNING id',
    [id, name, email, hashedPassword, role],
  );

  return res.status(201).json({
    status: 'success',
    message: 'User berhasil ditambahkan',
    data: { id: result.rows[0].id },
  });
}));

/* GET /users/:id - detail pengguna, dilayani cache (password tidak pernah dikirim) */
router.get('/:id', cache((req) => cacheKeys.user(req.params.id)), asyncHandler(async (req, res) => {
  const result = await pool.query(
    'SELECT id, name, email, role FROM users WHERE id = $1',
    [req.params.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('User tidak ditemukan');
  }

  return res.status(200).json({ status: 'success', data: result.rows[0] });
}));

/* PUT /users/:id - memperbarui data diri, lalu membatalkan cache detail pengguna */
router.put('/:id', auth, validate(UserUpdatePayloadSchema), asyncHandler(async (req, res) => {
  /* Pengguna hanya boleh memperbarui datanya sendiri. */
  if (req.user.id !== req.params.id) {
    throw new AuthorizationError('Anda tidak berhak memperbarui data pengguna ini');
  }

  const { name, email } = req.body;

  const result = await pool.query(
    `UPDATE users SET name = $1, email = $2, updated_at = current_timestamp
     WHERE id = $3 RETURNING id`,
    [name, email, req.params.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('User tidak ditemukan');
  }

  await cacheService.delete(cacheKeys.user(req.params.id));

  return res.status(200).json({ status: 'success', message: 'User berhasil diperbarui' });
}));

module.exports = router;
