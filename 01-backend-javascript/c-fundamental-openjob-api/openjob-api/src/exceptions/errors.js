/* Kumpulan custom error agar status code konsisten di seluruh aplikasi. */
class ClientError extends Error {
  constructor(message, statusCode = 400) {
    super(message);
    this.statusCode = statusCode;
    this.name = 'ClientError';
  }
}

/* 400 - payload tidak valid / permintaan salah */
class InvariantError extends ClientError {
  constructor(message) { super(message, 400); this.name = 'InvariantError'; }
}

/* 404 - resource tidak ditemukan */
class NotFoundError extends ClientError {
  constructor(message) { super(message, 404); this.name = 'NotFoundError'; }
}

/* 401 - kredensial / token tidak valid */
class AuthenticationError extends ClientError {
  constructor(message) { super(message, 401); this.name = 'AuthenticationError'; }
}

/* 403 - tidak berhak mengakses resource */
class AuthorizationError extends ClientError {
  constructor(message) { super(message, 403); this.name = 'AuthorizationError'; }
}

module.exports = { ClientError, InvariantError, NotFoundError, AuthenticationError, AuthorizationError };
