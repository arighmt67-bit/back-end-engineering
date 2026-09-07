import express from 'express';
import authenticationMiddleware from '../../middlewares/authenticationMiddleware.js';

const createThreadsRouter = (handler) => {
  const router = express.Router();

  router.post('/', authenticationMiddleware, handler.postThreadHandler);
  router.get('/:threadId', handler.getThreadDetailHandler);

  return router;
};

export default createThreadsRouter;
