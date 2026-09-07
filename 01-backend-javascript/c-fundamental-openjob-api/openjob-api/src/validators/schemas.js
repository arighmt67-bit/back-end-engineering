/* Skema validasi Joi untuk seluruh resource. */
const Joi = require('joi');

/* Registrasi user: password minimal 6 karakter.
   Properti role bersifat opsional dan default-nya 'user'. */
const UserPayloadSchema = Joi.object({
  name: Joi.string().required(),
  email: Joi.string().email().required(),
  password: Joi.string().min(6).required(),
  role: Joi.string().valid('user', 'admin').default('user'),
});

/* PENTING: skema login TIDAK memvalidasi panjang password.
   Password salah (walau pendek) harus dijawab 401 Unauthorized,
   bukan 400 Bad Request. */
const LoginPayloadSchema = Joi.object({
  email: Joi.string().email().required(),
  password: Joi.string().required(),
});

/* Pembaruan data diri pengguna: password tidak ikut diubah di sini. */
const UserUpdatePayloadSchema = Joi.object({
  name: Joi.string().required(),
  email: Joi.string().email().required(),
});

const RefreshTokenPayloadSchema = Joi.object({
  refreshToken: Joi.string().required(),
});

const DeleteAuthenticationPayloadSchema = Joi.object({
  refreshToken: Joi.string().required(),
});

const CompanyPayloadSchema = Joi.object({
  name: Joi.string().required(),
  location: Joi.string().required(),
  description: Joi.string().allow('', null),
});

const CategoryPayloadSchema = Joi.object({
  name: Joi.string().min(1).required(),
  description: Joi.string().allow('', null),
});

const JobPayloadSchema = Joi.object({
  company_id: Joi.string().required(),
  category_id: Joi.string().allow('', null),
  title: Joi.string().required(),
  description: Joi.string().allow('', null),
  job_type: Joi.string().allow('', null),
  experience_level: Joi.string().allow('', null),
  location_type: Joi.string().allow('', null),
  location_city: Joi.string().allow('', null),
  salary_min: Joi.number().allow(null),
  salary_max: Joi.number().allow(null),
  is_salary_visible: Joi.boolean(),
  status: Joi.string().allow('', null),
});

/* Skema pembaruan lowongan: seluruh properti bersifat opsional (parsial),
   sehingga permintaan yang hanya mengubah sebagian field tetap valid.
   Minimal satu properti wajib dikirim agar permintaan kosong tetap ditolak. */
const JobUpdatePayloadSchema = Joi.object({
  company_id: Joi.string(),
  category_id: Joi.string().allow('', null),
  title: Joi.string(),
  description: Joi.string().allow('', null),
  job_type: Joi.string().allow('', null),
  experience_level: Joi.string().allow('', null),
  location_type: Joi.string().allow('', null),
  location_city: Joi.string().allow('', null),
  salary_min: Joi.number().allow(null),
  salary_max: Joi.number().allow(null),
  is_salary_visible: Joi.boolean(),
  status: Joi.string().allow('', null),
}).min(1);

const ApplicationPayloadSchema = Joi.object({
  user_id: Joi.string().required(),
  job_id: Joi.string().required(),
  status: Joi.string().allow('', null),
});

const ApplicationStatusPayloadSchema = Joi.object({
  status: Joi.string().required(),
});

module.exports = {
  UserPayloadSchema,
  UserUpdatePayloadSchema,
  LoginPayloadSchema,
  RefreshTokenPayloadSchema,
  DeleteAuthenticationPayloadSchema,
  CompanyPayloadSchema,
  CategoryPayloadSchema,
  JobPayloadSchema,
  JobUpdatePayloadSchema,
  ApplicationPayloadSchema,
  ApplicationStatusPayloadSchema,
};
