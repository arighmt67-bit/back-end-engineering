/* Route GET /bookmarks - daftar bookmark milik pengguna yang sedang login (dilayani cache). */
const express = require('express');
const pool = require('../config/database');
const auth = require('../middlewares/auth');
const cache = require('../middlewares/cache');
const asyncHandler = require('../utils/asyncHandler');
const { cacheKeys } = require('../services/cacheService');

const router = express.Router();

router.get(
  '/',
  auth,
  cache((req) => cacheKeys.bookmarks(req.user.id)),
  asyncHandler(async (req, res) => {
    const result = await pool.query(
      `SELECT bookmarks.*,
              jobs.title, jobs.description, jobs.job_type, jobs.experience_level,
              jobs.location_type, jobs.location_city, jobs.salary_min, jobs.salary_max,
              jobs.is_salary_visible, jobs.status, jobs.company_id, jobs.category_id,
              companies.name AS company_name, companies.location AS company_location
       FROM bookmarks
       JOIN jobs ON bookmarks.job_id = jobs.id
       JOIN companies ON jobs.company_id = companies.id
       WHERE bookmarks.user_id = $1
       ORDER BY bookmarks.created_at DESC`,
      [req.user.id],
    );
    return res.status(200).json({ status: 'success', data: { bookmarks: result.rows } });
  }),
);

module.exports = router;
