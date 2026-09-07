/* Route resource documents: unggah, tampilkan, dan hapus berkas CV/Resume berformat PDF. */
const express = require('express');
const path = require('path');
const fs = require('fs');
const { nanoid } = require('nanoid');
const pool = require('../config/database');
const auth = require('../middlewares/auth');
const upload = require('../middlewares/upload');
const asyncHandler = require('../utils/asyncHandler');
const { InvariantError, NotFoundError } = require('../exceptions/errors');

const router = express.Router();
const DIREKTORI = path.resolve(__dirname, '..', '..', 'uploads');

/* POST /documents - mengunggah berkas PDF (maksimal 5 MB) */
router.post('/', auth, upload, asyncHandler(async (req, res) => {
  /* Berlaku untuk permintaan tanpa berkas maupun berkas yang ditolak filter MIME. */
  if (!req.file) {
    throw new InvariantError('File is required and must be a PDF document');
  }

  const id = `document-${nanoid(16)}`;
  const { filename, originalname, mimetype, size } = req.file;

  await pool.query(
    `INSERT INTO documents (id, user_id, filename, original_name, mime_type, size)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [id, req.user.id, filename, originalname, mimetype, size],
  );

  return res.status(201).json({
    status: 'success',
    message: 'Document berhasil diunggah',
    data: {
      documentId: id,
      filename,
      originalName: originalname,
      size,
    },
  });
}));

/* GET /documents - daftar seluruh berkas yang tersimpan */
router.get('/', asyncHandler(async (_req, res) => {
  const result = await pool.query(
    `SELECT id, user_id, filename, original_name, mime_type, size, created_at
     FROM documents ORDER BY created_at DESC`,
  );
  return res.status(200).json({ status: 'success', data: { documents: result.rows } });
}));

/* GET /documents/:id - menampilkan berkas PDF yang telah diunggah */
router.get('/:id', asyncHandler(async (req, res) => {
  const result = await pool.query('SELECT * FROM documents WHERE id = $1', [req.params.id]);
  if (result.rowCount === 0) {
    throw new NotFoundError('Document tidak ditemukan');
  }

  const dokumen = result.rows[0];
  const lokasi = path.join(DIREKTORI, dokumen.filename);

  if (!fs.existsSync(lokasi)) {
    throw new NotFoundError('Berkas fisik dokumen tidak ditemukan');
  }

  res.setHeader('Content-Type', dokumen.mime_type);
  res.setHeader('Content-Disposition', `inline; filename="${dokumen.original_name}"`);
  return res.status(200).sendFile(lokasi);
}));

/* DELETE /documents/:id - menghapus berkas beserta metadatanya */
router.delete('/:id', auth, asyncHandler(async (req, res) => {
  const result = await pool.query(
    'DELETE FROM documents WHERE id = $1 RETURNING filename',
    [req.params.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('Document tidak ditemukan');
  }

  const lokasi = path.join(DIREKTORI, result.rows[0].filename);
  if (fs.existsSync(lokasi)) {
    fs.unlinkSync(lokasi);
  }

  return res.status(200).json({ status: 'success', message: 'Document berhasil dihapus' });
}));

module.exports = router;
