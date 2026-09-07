/* Route resource companies. Detail perusahaan disajikan lewat cache Redis. */
const express = require('express');
const { nanoid } = require('nanoid');
const pool = require('../config/database');
const validate = require('../middlewares/validate');
const auth = require('../middlewares/auth');
const cache = require('../middlewares/cache');
const asyncHandler = require('../utils/asyncHandler');
const { CompanyPayloadSchema } = require('../validators/schemas');
const { NotFoundError } = require('../exceptions/errors');
const { cacheService, cacheKeys } = require('../services/cacheService');

const router = express.Router();

/* POST /companies - menambah perusahaan (perlu autentikasi).
   Pengguna yang membuat perusahaan dicatat sebagai pemiliknya. */
router.post('/', auth, validate(CompanyPayloadSchema), asyncHandler(async (req, res) => {
  const { name, location, description = null } = req.body;
  const id = `company-${nanoid(16)}`;

  const result = await pool.query(
    `INSERT INTO companies (id, name, location, description, owner_id)
     VALUES ($1, $2, $3, $4, $5) RETURNING id`,
    [id, name, location, description, req.user.id],
  );

  return res.status(201).json({
    status: 'success',
    message: 'Company berhasil ditambahkan',
    data: { id: result.rows[0].id },
  });
}));

/* GET /companies - daftar seluruh perusahaan.
   Kolom owner_id tidak ditampilkan agar bentuk response tetap konsisten. */
router.get('/', asyncHandler(async (_req, res) => {
  const result = await pool.query(
    `SELECT id, name, location, description, created_at, updated_at
     FROM companies ORDER BY created_at DESC`,
  );
  return res.status(200).json({ status: 'success', data: { companies: result.rows } });
}));

/* GET /companies/:id - detail perusahaan (dilayani cache selama 1 jam) */
router.get('/:id', cache((req) => cacheKeys.company(req.params.id)), asyncHandler(async (req, res) => {
  const result = await pool.query(
    `SELECT id, name, location, description, created_at, updated_at
     FROM companies WHERE id = $1`,
    [req.params.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('Company tidak ditemukan');
  }

  return res.status(200).json({ status: 'success', data: result.rows[0] });
}));

/* PUT /companies/:id - memperbarui perusahaan, lalu membatalkan cache detailnya */
router.put('/:id', auth, validate(CompanyPayloadSchema), asyncHandler(async (req, res) => {
  const { name, location, description = null } = req.body;

  const result = await pool.query(
    `UPDATE companies SET name = $1, location = $2, description = $3,
     updated_at = current_timestamp WHERE id = $4 RETURNING id`,
    [name, location, description, req.params.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('Company tidak ditemukan');
  }

  await cacheService.delete(cacheKeys.company(req.params.id));

  return res.status(200).json({ status: 'success', message: 'Company berhasil diperbarui' });
}));

/* DELETE /companies/:id - menghapus perusahaan beserta cache detailnya */
router.delete('/:id', auth, asyncHandler(async (req, res) => {
  const result = await pool.query('DELETE FROM companies WHERE id = $1 RETURNING id', [req.params.id]);
  if (result.rowCount === 0) {
    throw new NotFoundError('Company tidak ditemukan');
  }

  await cacheService.delete(cacheKeys.company(req.params.id));

  return res.status(200).json({ status: 'success', message: 'Company berhasil dihapus' });
}));

module.exports = router;
