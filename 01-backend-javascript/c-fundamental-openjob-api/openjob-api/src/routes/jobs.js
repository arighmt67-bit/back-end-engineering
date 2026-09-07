/* Route resource jobs, termasuk pencarian dan bookmark bersarang. */
const express = require('express');
const { nanoid } = require('nanoid');
const pool = require('../config/database');
const validate = require('../middlewares/validate');
const auth = require('../middlewares/auth');
const asyncHandler = require('../utils/asyncHandler');
const { JobPayloadSchema, JobUpdatePayloadSchema } = require('../validators/schemas');
const { NotFoundError, InvariantError } = require('../exceptions/errors');
const { cacheService, cacheKeys } = require('../services/cacheService');

const router = express.Router();

/* POST /jobs - menambah lowongan */
router.post('/', auth, validate(JobPayloadSchema), asyncHandler(async (req, res) => {
  const {
    company_id, category_id = null, title, description = null, job_type = null,
    experience_level = null, location_type = null, location_city = null,
    salary_min = null, salary_max = null, is_salary_visible = true, status = 'open',
  } = req.body;

  /* company_id wajib merujuk perusahaan yang benar-benar ada. */
  const company = await pool.query('SELECT id FROM companies WHERE id = $1', [company_id]);
  if (company.rowCount === 0) {
    throw new InvariantError('Company tidak ditemukan');
  }

  const id = `job-${nanoid(16)}`;
  const result = await pool.query(
    `INSERT INTO jobs (id, company_id, category_id, title, description, job_type,
     experience_level, location_type, location_city, salary_min, salary_max,
     is_salary_visible, status)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13) RETURNING id`,
    [id, company_id, category_id || null, title, description, job_type,
      experience_level, location_type, location_city, salary_min, salary_max,
      is_salary_visible, status],
  );

  return res.status(201).json({
    status: 'success',
    message: 'Job berhasil ditambahkan',
    data: { id: result.rows[0].id },
  });
}));

/* GET /jobs - daftar lowongan dengan pencarian opsional
   ?title=...  dan  ?company-name=...  (pencarian tidak peka huruf besar/kecil) */
router.get('/', asyncHandler(async (req, res) => {
  const title = req.query.title;
  const companyName = req.query['company-name'];

  const conditions = [];
  const values = [];

  if (title) {
    values.push(`%${title}%`);
    conditions.push(`jobs.title ILIKE $${values.length}`);
  }
  if (companyName) {
    values.push(`%${companyName}%`);
    conditions.push(`companies.name ILIKE $${values.length}`);
  }

  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';

  /* Kolom company_name hanya disertakan saat pencarian dilakukan, sehingga
     bentuk response daftar lowongan biasa tetap konsisten dengan skema jobs. */
  const kolomDasar = `jobs.id, jobs.company_id, jobs.category_id, jobs.title, jobs.description,
     jobs.job_type, jobs.experience_level, jobs.location_type, jobs.location_city,
     jobs.salary_min, jobs.salary_max, jobs.is_salary_visible, jobs.status`;

  const kolom = conditions.length
    ? `${kolomDasar}, companies.name AS company_name`
    : kolomDasar;

  const result = await pool.query(
    `SELECT ${kolom} FROM jobs
     LEFT JOIN companies ON jobs.company_id = companies.id
     ${where} ORDER BY jobs.created_at DESC`,
    values,
  );

  return res.status(200).json({ status: 'success', data: { jobs: result.rows } });
}));

/* GET /jobs/company/:companyId - lowongan berdasarkan perusahaan.
   Id yang tidak dikenal tetap dijawab 200 dengan array kosong (bukan 404). */
router.get('/company/:companyId', asyncHandler(async (req, res) => {
  const result = await pool.query(
    'SELECT * FROM jobs WHERE company_id = $1 ORDER BY created_at DESC',
    [req.params.companyId],
  );
  return res.status(200).json({ status: 'success', data: { jobs: result.rows } });
}));

/* GET /jobs/category/:categoryId - lowongan berdasarkan kategori (perilaku sama seperti di atas). */
router.get('/category/:categoryId', asyncHandler(async (req, res) => {
  const result = await pool.query(
    'SELECT * FROM jobs WHERE category_id = $1 ORDER BY created_at DESC',
    [req.params.categoryId],
  );
  return res.status(200).json({ status: 'success', data: { jobs: result.rows } });
}));

/* --- Bookmark bersarang: /jobs/:jobId/bookmark --- */

/* POST /jobs/:jobId/bookmark - menyimpan lowongan */
router.post('/:jobId/bookmark', auth, asyncHandler(async (req, res) => {
  const { jobId } = req.params;

  const job = await pool.query('SELECT id FROM jobs WHERE id = $1', [jobId]);
  if (job.rowCount === 0) {
    throw new NotFoundError('Job tidak ditemukan');
  }

  const id = `bookmark-${nanoid(16)}`;
  const result = await pool.query(
    'INSERT INTO bookmarks (id, user_id, job_id) VALUES ($1, $2, $3) RETURNING id',
    [id, req.user.id, jobId],
  );

  /* Daftar bookmark milik pengguna berubah, cache-nya dibatalkan. */
  await cacheService.delete(cacheKeys.bookmarks(req.user.id));

  return res.status(201).json({
    status: 'success',
    message: 'Bookmark berhasil ditambahkan',
    data: { id: result.rows[0].id },
  });
}));

/* GET /jobs/:jobId/bookmark/:bookmarkId - detail bookmark */
router.get('/:jobId/bookmark/:bookmarkId', auth, asyncHandler(async (req, res) => {
  const result = await pool.query(
    'SELECT * FROM bookmarks WHERE id = $1 AND job_id = $2',
    [req.params.bookmarkId, req.params.jobId],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('Bookmark tidak ditemukan');
  }

  return res.status(200).json({ status: 'success', data: result.rows[0] });
}));

/* DELETE /jobs/:jobId/bookmark - menghapus bookmark milik pengguna */
router.delete('/:jobId/bookmark', auth, asyncHandler(async (req, res) => {
  const result = await pool.query(
    'DELETE FROM bookmarks WHERE job_id = $1 AND user_id = $2 RETURNING id',
    [req.params.jobId, req.user.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('Bookmark tidak ditemukan');
  }

  await cacheService.delete(cacheKeys.bookmarks(req.user.id));

  return res.status(200).json({ status: 'success', message: 'Bookmark berhasil dihapus' });
}));

/* GET /jobs/:id - detail lowongan (didaftarkan setelah route spesifik di atas) */
router.get('/:id', asyncHandler(async (req, res) => {
  const result = await pool.query('SELECT * FROM jobs WHERE id = $1', [req.params.id]);
  if (result.rowCount === 0) {
    throw new NotFoundError('Job tidak ditemukan');
  }
  return res.status(200).json({ status: 'success', data: result.rows[0] });
}));

/* PUT /jobs/:id - memperbarui lowongan.
   Pembaruan bersifat parsial: hanya properti yang dikirim yang diubah,
   properti lain dipertahankan nilainya melalui COALESCE. */
router.put('/:id', auth, validate(JobUpdatePayloadSchema), asyncHandler(async (req, res) => {
  const {
    company_id, category_id, title, description, job_type,
    experience_level, location_type, location_city,
    salary_min, salary_max, is_salary_visible, status,
  } = req.body;

  const nilai = (v) => (v === undefined ? null : v);

  const result = await pool.query(
    `UPDATE jobs SET
       company_id=COALESCE($1, company_id),
       category_id=COALESCE($2, category_id),
       title=COALESCE($3, title),
       description=COALESCE($4, description),
       job_type=COALESCE($5, job_type),
       experience_level=COALESCE($6, experience_level),
       location_type=COALESCE($7, location_type),
       location_city=COALESCE($8, location_city),
       salary_min=COALESCE($9, salary_min),
       salary_max=COALESCE($10, salary_max),
       is_salary_visible=COALESCE($11, is_salary_visible),
       status=COALESCE($12, status),
       updated_at=current_timestamp
     WHERE id=$13 RETURNING id`,
    [nilai(company_id), nilai(category_id), nilai(title), nilai(description),
      nilai(job_type), nilai(experience_level), nilai(location_type),
      nilai(location_city), nilai(salary_min), nilai(salary_max),
      nilai(is_salary_visible), nilai(status), req.params.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('Job tidak ditemukan');
  }

  return res.status(200).json({ status: 'success', message: 'Job berhasil diperbarui' });
}));

/* DELETE /jobs/:id - menghapus lowongan */
router.delete('/:id', auth, asyncHandler(async (req, res) => {
  const result = await pool.query('DELETE FROM jobs WHERE id = $1 RETURNING id', [req.params.id]);
  if (result.rowCount === 0) {
    throw new NotFoundError('Job tidak ditemukan');
  }
  return res.status(200).json({ status: 'success', message: 'Job berhasil dihapus' });
}));

module.exports = router;
