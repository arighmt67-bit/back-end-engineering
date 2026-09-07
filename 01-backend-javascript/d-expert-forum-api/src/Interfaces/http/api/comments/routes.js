import express from 'express';
import authenticationMiddleware from '../../middlewares/authenticationMiddleware.js';

const createCommentsRouter = (handler) => {
  const router = express.Router({ mergeParams: true });

  router.post('/', authenticationMiddleware, handler.postCommentHandler);
  router.delete('/:commentId', authenticationMiddleware, handler.deleteCommentHandler);

  return router;
};

export default createCommentsRouter;
