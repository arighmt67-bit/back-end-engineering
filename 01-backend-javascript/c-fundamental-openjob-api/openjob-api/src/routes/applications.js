/* Route resource applications (lamaran pekerjaan). Seluruh endpoint perlu autentikasi.
   Menerapkan cache Redis dan publikasi pesan ke RabbitMQ saat lamaran dibuat. */
const express = require('express');
const { nanoid } = require('nanoid');
const pool = require('../config/database');
const validate = require('../middlewares/validate');
const auth = require('../middlewares/auth');
const cache = require('../middlewares/cache');
const asyncHandler = require('../utils/asyncHandler');
const { ApplicationPayloadSchema, ApplicationStatusPayloadSchema } = require('../validators/schemas');
const { NotFoundError, InvariantError } = require('../exceptions/errors');
const { cacheService, cacheKeys } = require('../services/cacheService');
const ProducerService = require('../services/producerService');

const router = express.Router();

/* Membatalkan seluruh cache yang terpengaruh oleh perubahan sebuah lamaran. */
const batalkanCacheLamaran = async ({ id, userId, jobId }) => {
  await cacheService.delete(
    id ? cacheKeys.application(id) : null,
    userId ? cacheKeys.applicationsByUser(userId) : null,
    jobId ? cacheKeys.applicationsByJob(jobId) : null,
  );
};

/* POST /applications - melamar pekerjaan */
router.post('/', auth, validate(ApplicationPayloadSchema), asyncHandler(async (req, res) => {
  const { user_id, job_id, status = 'pending' } = req.body;

  const job = await pool.query('SELECT id FROM jobs WHERE id = $1', [job_id]);
  if (job.rowCount === 0) {
    throw new InvariantError('Job tidak ditemukan');
  }

  /* Satu kandidat tidak boleh melamar lowongan yang sama dua kali,
     agar tidak terjadi publikasi pesan ganda ke RabbitMQ. */
  const duplikat = await pool.query(
    'SELECT id FROM applications WHERE user_id = $1 AND job_id = $2',
    [user_id, job_id],
  );
  if (duplikat.rowCount > 0) {
    throw new InvariantError('Anda sudah pernah melamar pekerjaan ini');
  }

  const id = `application-${nanoid(16)}`;
  const result = await pool.query(
    `INSERT INTO applications (id, user_id, job_id, status)
     VALUES ($1,$2,$3,$4) RETURNING id, user_id, job_id, status`,
    [id, user_id, job_id, status],
  );

  const lamaran = result.rows[0];

  /* Cache daftar lamaran wajib dibatalkan agar data baru langsung terlihat. */
  await batalkanCacheLamaran({ userId: lamaran.user_id, jobId: lamaran.job_id });

  /* Pesan dikirim tanpa ditunggu supaya respons API tetap cepat. */
  ProducerService.kirimLamaranBaru(lamaran.id);

  return res.status(201).json({
    status: 'success',
    message: 'Application berhasil ditambahkan',
    data: lamaran,
  });
}));

/* GET /applications - seluruh lamaran */
router.get('/', auth, asyncHandler(async (_req, res) => {
  const result = await pool.query(
    `SELECT applications.*,
            users.name AS user_name, users.email AS user_email,
            jobs.title AS job_title,
            companies.name AS company_name,
            jobs.location_city, jobs.location_type, jobs.job_type
     FROM applications
     JOIN users ON applications.user_id = users.id
     JOIN jobs ON applications.job_id = jobs.id
     JOIN companies ON jobs.company_id = companies.id
     ORDER BY applications.created_at DESC`,
  );
  return res.status(200).json({ status: 'success', data: { applications: result.rows } });
}));

/* GET /applications/user/:userId - lamaran berdasarkan pengguna (dilayani cache).
   Id yang tidak dikenal tetap dijawab 200 dengan array kosong. */
router.get(
  '/user/:userId',
  auth,
  cache((req) => cacheKeys.applicationsByUser(req.params.userId)),
  asyncHandler(async (req, res) => {
    const result = await pool.query(
      'SELECT * FROM applications WHERE user_id = $1 ORDER BY created_at DESC',
      [req.params.userId],
    );
    return res.status(200).json({ status: 'success', data: { applications: result.rows } });
  }),
);

/* GET /applications/job/:jobId - lamaran berdasarkan lowongan (perilaku sama seperti di atas). */
router.get(
  '/job/:jobId',
  auth,
  cache((req) => cacheKeys.applicationsByJob(req.params.jobId)),
  asyncHandler(async (req, res) => {
    const result = await pool.query(
      'SELECT * FROM applications WHERE job_id = $1 ORDER BY created_at DESC',
      [req.params.jobId],
    );
    return res.status(200).json({ status: 'success', data: { applications: result.rows } });
  }),
);

/* GET /applications/:id - detail lamaran (dilayani cache) */
router.get(
  '/:id',
  auth,
  cache((req) => cacheKeys.application(req.params.id)),
  asyncHandler(async (req, res) => {
    const result = await pool.query('SELECT * FROM applications WHERE id = $1', [req.params.id]);
    if (result.rowCount === 0) {
      throw new NotFoundError('Application tidak ditemukan');
    }
    return res.status(200).json({ status: 'success', data: result.rows[0] });
  }),
);

/* PUT /applications/:id - memperbarui status lamaran, lalu membatalkan cache terkait */
router.put('/:id', auth, validate(ApplicationStatusPayloadSchema), asyncHandler(async (req, res) => {
  const result = await pool.query(
    `UPDATE applications SET status = $1, updated_at = current_timestamp
     WHERE id = $2 RETURNING id, user_id, job_id`,
    [req.body.status, req.params.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('Application tidak ditemukan');
  }

  const { id, user_id: userId, job_id: jobId } = result.rows[0];
  await batalkanCacheLamaran({ id, userId, jobId });

  return res.status(200).json({ status: 'success', message: 'Application berhasil diperbarui' });
}));

/* DELETE /applications/:id - membatalkan lamaran beserta cache-nya */
router.delete('/:id', auth, asyncHandler(async (req, res) => {
  const result = await pool.query(
    'DELETE FROM applications WHERE id = $1 RETURNING id, user_id, job_id',
    [req.params.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('Application tidak ditemukan');
  }

  const { id, user_id: userId, job_id: jobId } = result.rows[0];
  await batalkanCacheLamaran({ id, userId, jobId });

  return res.status(200).json({ status: 'success', message: 'Application berhasil dihapus' });
}));

module.exports = router;
