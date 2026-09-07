/* istanbul ignore file */
import jwt from 'jsonwebtoken';
import config from '../src/Commons/config.js';

const ServerTestHelper = {
  /** Membuat access token untuk keperluan functional test. */
  getAccessToken({ id = 'user-123', username = 'dicoding' } = {}) {
    return jwt.sign({ id, username }, config.auth.accessTokenKey);
  },
};

export default ServerTestHelper;
