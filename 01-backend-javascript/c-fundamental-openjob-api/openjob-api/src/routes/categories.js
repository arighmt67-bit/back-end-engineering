/* Route resource categories. */
const express = require('express');
const { nanoid } = require('nanoid');
const pool = require('../config/database');
const validate = require('../middlewares/validate');
const auth = require('../middlewares/auth');
const asyncHandler = require('../utils/asyncHandler');
const { CategoryPayloadSchema } = require('../validators/schemas');
const { NotFoundError } = require('../exceptions/errors');

const router = express.Router();

/* POST /categories - menambah kategori */
router.post('/', auth, validate(CategoryPayloadSchema), asyncHandler(async (req, res) => {
  const { name, description = null } = req.body;
  const id = `category-${nanoid(16)}`;

  const result = await pool.query(
    'INSERT INTO categories (id, name, description) VALUES ($1, $2, $3) RETURNING id',
    [id, name, description],
  );

  return res.status(201).json({
    status: 'success',
    message: 'Category berhasil ditambahkan',
    data: { id: result.rows[0].id },
  });
}));

/* GET /categories - daftar seluruh kategori */
router.get('/', asyncHandler(async (_req, res) => {
  /* Hanya empat kolom inti yang ditampilkan pada daftar kategori. */
  const result = await pool.query(
    'SELECT id, name, description, created_at FROM categories ORDER BY created_at DESC',
  );
  return res.status(200).json({ status: 'success', data: { categories: result.rows } });
}));

/* GET /categories/:id - detail kategori */
router.get('/:id', asyncHandler(async (req, res) => {
  const result = await pool.query('SELECT * FROM categories WHERE id = $1', [req.params.id]);
  if (result.rowCount === 0) {
    throw new NotFoundError('Category tidak ditemukan');
  }
  return res.status(200).json({ status: 'success', data: result.rows[0] });
}));

/* PUT /categories/:id - memperbarui kategori */
router.put('/:id', auth, validate(CategoryPayloadSchema), asyncHandler(async (req, res) => {
  const { name, description = null } = req.body;

  const result = await pool.query(
    `UPDATE categories SET name = $1, description = $2,
     updated_at = current_timestamp WHERE id = $3 RETURNING id`,
    [name, description, req.params.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('Category tidak ditemukan');
  }

  return res.status(200).json({ status: 'success', message: 'Category berhasil diperbarui' });
}));

/* DELETE /categories/:id - menghapus kategori */
router.delete('/:id', auth, asyncHandler(async (req, res) => {
  const result = await pool.query('DELETE FROM categories WHERE id = $1 RETURNING id', [req.params.id]);
  if (result.rowCount === 0) {
    throw new NotFoundError('Category tidak ditemukan');
  }
  return res.status(200).json({ status: 'success', message: 'Category berhasil dihapus' });
}));

module.exports = router;
