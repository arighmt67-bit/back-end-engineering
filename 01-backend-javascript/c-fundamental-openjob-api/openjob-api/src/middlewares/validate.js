/* Middleware validasi data menggunakan Joi.
   convert:false -> nilai bertipe salah (mis. name berupa angka) TIDAK dikonversi,
   sehingga tetap dianggap tidak valid dan menghasilkan 400. */
const { InvariantError } = require('../exceptions/errors');

const validate = (schema) => (req, _res, next) => {
  const { error } = schema.validate(req.body, { convert: false, abortEarly: true });
  if (error) {
    return next(new InvariantError(error.details[0].message));
  }
  return next();
};

module.exports = validate;
