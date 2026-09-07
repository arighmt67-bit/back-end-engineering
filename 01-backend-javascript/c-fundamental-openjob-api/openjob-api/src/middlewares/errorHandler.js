/* Middleware error handling terpusat. */
const { ClientError } = require('../exceptions/errors');

// eslint-disable-next-line no-unused-vars
const errorHandler = (err, _req, res, _next) => {
  if (err instanceof ClientError) {
    return res.status(err.statusCode).json({ status: 'failed', message: err.message });
  }
  console.error(err);
  return res.status(500).json({ status: 'failed', message: 'Terjadi kegagalan pada server kami' });
};

/* Handler untuk route yang tidak terdaftar */
const notFoundHandler = (_req, res) =>
  res.status(404).json({ status: 'failed', message: 'Resource yang Anda minta tidak ditemukan' });

module.exports = { errorHandler, notFoundHandler };
