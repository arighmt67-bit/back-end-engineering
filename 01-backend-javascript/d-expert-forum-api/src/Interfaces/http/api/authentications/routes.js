import express from 'express';

const createAuthenticationsRouter = (handler) => {
  const router = express.Router();

  router.post('/', handler.postAuthenticationHandler);
  router.put('/', handler.putAuthenticationHandler);
  router.delete('/', handler.deleteAuthenticationHandler);

  return router;
};

export default createAuthenticationsRouter;
