import jwt from 'jsonwebtoken';
import config from '../../../Commons/config.js';
import AuthenticationError from '../../../Commons/exceptions/AuthenticationError.js';

/**
 * Middleware autentikasi diletakkan pada layer Interface.
 * Dengan begitu use case tetap bersih dan dapat dipakai ulang
 * meskipun aplikasi berganti antarmuka (misalnya CLI).
 */
const authenticationMiddleware = (req, res, next) => {
  try {
    const authorization = req.headers.authorization;

    if (!authorization || !authorization.startsWith('Bearer ')) {
      throw new AuthenticationError('Missing authentication');
    }

    const token = authorization.split(' ')[1];
    const payload = jwt.verify(token, config.auth.accessTokenKey);

    req.auth = { credentials: { id: payload.id, username: payload.username } };

    return next();
  } catch (error) {
    if (error instanceof AuthenticationError) {
      return next(error);
    }

    return next(new AuthenticationError('Missing authentication'));
  }
};

export default authenticationMiddleware;
