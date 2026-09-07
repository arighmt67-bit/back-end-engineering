/* Route resource profile: data milik pengguna yang sedang login. */
const express = require('express');
const pool = require('../config/database');
const auth = require('../middlewares/auth');
const asyncHandler = require('../utils/asyncHandler');
const { NotFoundError } = require('../exceptions/errors');

const router = express.Router();

/* GET /profile - profil pengguna */
router.get('/', auth, asyncHandler(async (req, res) => {
  const result = await pool.query(
    'SELECT id, name, email, role FROM users WHERE id = $1',
    [req.user.id],
  );

  if (result.rowCount === 0) {
    throw new NotFoundError('User tidak ditemukan');
  }

  return res.status(200).json({ status: 'success', data: result.rows[0] });
}));

/* GET /profile/applications - lamaran milik pengguna, dilengkapi detail lowongan */
router.get('/applications', auth, asyncHandler(async (req, res) => {
  const result = await pool.query(
    `SELECT applications.*,
            jobs.title AS job_title,
            jobs.job_type, jobs.experience_level,
            jobs.location_type, jobs.location_city,
            companies.name AS company_name, companies.location AS company_location,
            users.name AS user_name, users.email AS user_email
     FROM applications
     JOIN jobs ON applications.job_id = jobs.id
     JOIN companies ON jobs.company_id = companies.id
     JOIN users ON applications.user_id = users.id
     WHERE applications.user_id = $1
     ORDER BY applications.created_at DESC`,
    [req.user.id],
  );
  return res.status(200).json({ status: 'success', data: { applications: result.rows } });
}));

/* GET /profile/bookmarks - bookmark milik pengguna */
router.get('/bookmarks', auth, asyncHandler(async (req, res) => {
  const result = await pool.query(
    'SELECT * FROM bookmarks WHERE user_id = $1 ORDER BY created_at DESC',
    [req.user.id],
  );
  return res.status(200).json({ status: 'success', data: { bookmarks: result.rows } });
}));

module.exports = router;
