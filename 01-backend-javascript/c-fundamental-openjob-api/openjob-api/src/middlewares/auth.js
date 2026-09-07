/* Middleware autentikasi: memverifikasi Access Token pada header Authorization. */
const jwt = require('jsonwebtoken');
const { AuthenticationError } = require('../exceptions/errors');

const authMiddleware = (req, _res, next) => {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return next(new AuthenticationError('Missing authentication'));
  }

  const token = authHeader.split(' ')[1];
  try {
    const payload = jwt.verify(token, process.env.ACCESS_TOKEN_KEY);
    req.user = { id: payload.id };  // payload token berisi id user
    return next();
  } catch (e) {
    return next(new AuthenticationError('Invalid authentication token'));
  }
};

module.exports = authMiddleware;
