import express from 'express';
import authenticationMiddleware from '../../middlewares/authenticationMiddleware.js';

const createLikesRouter = (handler) => {
  const router = express.Router({ mergeParams: true });

  router.put('/', authenticationMiddleware, handler.putLikeHandler);

  return router;
};

export default createLikesRouter;
